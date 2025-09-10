package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.request.StopRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.RoundTripFlightResponse;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.FlightMapper;
import iuh.fit.airsky.mapper.StopMapper;
import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.FlightTravelClass;
import iuh.fit.airsky.model.Gate;
import iuh.fit.airsky.model.Stop;
import iuh.fit.airsky.model.TravelClass;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.FlightService;
import iuh.fit.airsky.service.SeatService;
import iuh.fit.airsky.util.GenerateCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FlightServiceImpl implements FlightService {

    private static final int MIN_BUFFER_TIME_MINUTES = 30;

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final GateRepository gateRepository;
    private final GenerateCodeUtil generateCodeUtil;
    private final SeatService seatService;
    private final AircraftRepository aircraftRepository;
    private final StopRepository stopRepository;
    private final StopMapper stopMapper;
    private final TravelClassRepository travelClassRepository;

    public FlightServiceImpl(FlightRepository flightRepository, FlightMapper flightMapper,
                             AirlineRepository airlineRepository, AirportRepository airportRepository,
                             GateRepository gateRepository, GenerateCodeUtil generateCodeUtil,
                             SeatService seatService, AircraftRepository aircraftRepository,
                             StopRepository stopRepository, StopMapper stopMapper,
                             TravelClassRepository travelClassRepository) {
        this.flightRepository = flightRepository;
        this.flightMapper = flightMapper;
        this.airlineRepository = airlineRepository;
        this.airportRepository = airportRepository;
        this.gateRepository = gateRepository;
        this.generateCodeUtil = generateCodeUtil;
        this.seatService = seatService;
        this.aircraftRepository = aircraftRepository;
        this.stopRepository = stopRepository;
        this.stopMapper = stopMapper;
        this.travelClassRepository = travelClassRepository;
    }

    // Helper methods for schedule validation
    private boolean isAirportDepartureOverlap(Long airportId, LocalDateTime departureTime, LocalDateTime arrivalTime, Long excludeFlightId) {
        return flightRepository.existsByDepartureAirportIdAndTimeOverlap(airportId, departureTime, arrivalTime, excludeFlightId);
    }

    private boolean isAirportArrivalOverlap(Long airportId, LocalDateTime departureTime, LocalDateTime arrivalTime, Long excludeFlightId) {
        return flightRepository.existsByArrivalAirportIdAndTimeOverlap(airportId, departureTime, arrivalTime, excludeFlightId);
    }

    private boolean hasSufficientBufferTime(Long airportId, LocalDateTime time, int bufferMinutes) {
        LocalDateTime start = time.minusMinutes(bufferMinutes);
        LocalDateTime end = time.plusMinutes(bufferMinutes);
        return flightRepository.countFlightsAtAirportInTimeRange(airportId, start, end) > 0;
    }

    // Method helper để tính availableSeats tự động
    private Integer calculateAvailableSeats(Flight flight) {
        // Tính tổng ghế từ aircraft
        int totalSeats = flight.getAircraft().getTotalSeats();
        
        // Tính ghế đã đặt (giả sử có BookingRepository)
        // int bookedSeats = bookingRepository.countBookedSeatsByFlightId(flight.getFlightId());
        // return totalSeats - bookedSeats;
        
        // Tạm thời trả về totalSeats (có thể chỉnh sửa sau khi có BookingRepository)
        return totalSeats;
    }

    @Override
    public FlightResponse createFlight(FlightRequest request) {
        log.info("Creating new flight");

        // Validate input
        if (request.getDepartureTime().isAfter(request.getArrivalTime())) {
            throw new IllegalArgumentException("Departure time must be before arrival time");
        }

        // Validate round-trip: phải có đủ thông tin và hợp lệ
        if (request.getTripType() == iuh.fit.airsky.enums.TripType.ROUND_TRIP) {
            if (request.getRoundTripGroupId() == null || request.getRoundTripGroupId().isEmpty()) {
                throw new IllegalArgumentException("Round-trip flight must have a roundTripGroupId");
            }
            if (request.getDepartureAirportId().equals(request.getArrivalAirportId())) {
                throw new IllegalArgumentException("Departure and arrival airports must be different for round-trip");
            }
            // Có thể kiểm tra thêm: ngày đi và ngày về phải hợp lý nếu có thông tin chuyến về
        }

        // Validate multi-city: stopsList phải hợp lệ
        if (request.getTripType() == iuh.fit.airsky.enums.TripType.MULTI_CITY) {
            if (request.getStopsList() == null || request.getStopsList().isEmpty()) {
                throw new IllegalArgumentException("Multi-city flight must have at least one stop");
            }
            for (int i = 0; i < request.getStopsList().size(); i++) {
                var stop = request.getStopsList().get(i);
                if (stop.getStopOrder() == null || stop.getStopOrder() != i + 1) {
                    throw new IllegalArgumentException("Stop order must be consecutive starting from 1");
                }
                if (i > 0) {
                    var prev = request.getStopsList().get(i - 1);
                    if (stop.getAirportId().equals(prev.getAirportId())) {
                        throw new IllegalArgumentException("Consecutive stops cannot be at the same airport");
                    }
                    if (prev.getDepartureTime() != null && stop.getArrivalTime() != null &&
                        !stop.getArrivalTime().isAfter(prev.getDepartureTime())) {
                        throw new IllegalArgumentException("Each stop's arrival time must be after previous stop's departure time");
                    }
                }
                if (stop.getArrivalTime() != null && stop.getDepartureTime() != null &&
                    !stop.getDepartureTime().isAfter(stop.getArrivalTime())) {
                    throw new IllegalArgumentException("Stop departure time must be after arrival time");
                }
            }
        }

        // check airline id
        if (!airlineRepository.existsById(request.getAirlineId())) {
            throw new ResourceNotFoundException("Airline not found with id " + request.getAirlineId());
        }

        // Check for overlapping flights (aircraft)
        if (flightRepository.existsByAircraftIdAndTimeOverlap(
                request.getAircraftId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException("Aircraft is already scheduled for another flight in this time range");
        }

        // Check for overlapping flights (gate)
        if (flightRepository.existsByGateIdAndTimeOverlap(
                request.getGateId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException("Gate is already assigned to another flight in this time range");
        }

        // Check for overlapping flights at departure airport
        if (isAirportDepartureOverlap(request.getDepartureAirportId(), request.getDepartureTime(), request.getArrivalTime(), null)) {
            throw new IllegalArgumentException("Departure airport is busy with another flight in this time range");
        }

        // Check for overlapping flights at arrival airport
        if (isAirportArrivalOverlap(request.getArrivalAirportId(), request.getDepartureTime(), request.getArrivalTime(), null)) {
            throw new IllegalArgumentException("Arrival airport is busy with another flight in this time range");
        }

        // Check buffer time at airports
        if (hasSufficientBufferTime(request.getDepartureAirportId(), request.getDepartureTime(), MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Insufficient buffer time at departure airport");
        }
        if (hasSufficientBufferTime(request.getArrivalAirportId(), request.getArrivalTime(), MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Insufficient buffer time at arrival airport");
        }

        // Validate gate belongs to departure airport
        Gate gate = gateRepository.findById(request.getGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found"));
        if (!gate.getAirport().getAirportId().equals(request.getDepartureAirportId())) {
            throw new IllegalArgumentException("Gate must belong to the departure airport");
        }

        // Map DTO to entity
        Flight flight = flightMapper.toEntity(request);
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found"));
        flight.setAircraft(aircraft);

        // Set airline
        flight.setAirline(airlineRepository.findById(request.getAirlineId())
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id " + request.getAirlineId())));

        // Tự động tính duration từ departureTime và arrivalTime
        if (request.getDepartureTime() != null && request.getArrivalTime() != null) {
            flight.setDuration((int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());
        }

        String airlineCode = flight.getAirline().getAirlineCode();
        flight.setFlightNumber(generateCodeUtil.generateFlightNumber(flightRepository, airlineCode));
        flight.setDepartureAirport(airportRepository.findById(request.getDepartureAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure airport not found with id " + request.getDepartureAirportId())));
        flight.setArrivalAirport(airportRepository.findById(request.getArrivalAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Arrival airport not found with id " + request.getArrivalAirportId())));
        flight.setGate(gateRepository.findById(request.getGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found with id " + request.getGateId())));

        // Set flight travel classes if provided
        if (request.getFlightTravelClasses() != null && !request.getFlightTravelClasses().isEmpty()) {
            List<FlightTravelClass> flightTravelClasses = request.getFlightTravelClasses().stream()
                .map(ftcRequest -> {
                    FlightTravelClass ftc = new FlightTravelClass();
                    ftc.setFlight(flight);
                    TravelClass travelClass = travelClassRepository.findById(ftcRequest.getClassId())
                        .orElseThrow(() -> new IllegalArgumentException("Travel class not found with id " + ftcRequest.getClassId()));
                    ftc.setTravelClass(travelClass);
                    ftc.setCustomPrice(ftcRequest.getCustomPrice());
                    ftc.setAvailableSeats(ftcRequest.getAvailableSeats());
                    return ftc;
                })
                .collect(Collectors.toList());
            flight.setFlightTravelClasses(flightTravelClasses);
        }

        // Handle stops
        if (request.getStopsList() != null && !request.getStopsList().isEmpty()) {
            // Validate stop duration
            for (StopRequest stop : request.getStopsList()) {
                if (stop.getArrivalTime() != null && stop.getDepartureTime() != null) {
                    long stopDuration = java.time.Duration.between(stop.getArrivalTime(), stop.getDepartureTime()).toMinutes();
                    if (stopDuration < 20) {
                        throw new IllegalArgumentException("Stop duration must be at least 20 minutes");
                    }
                }
            }

            List<Stop> stops = request.getStopsList().stream().map(stopRequest -> {
                Stop stop = stopMapper.toEntity(stopRequest);
                Airport airport = airportRepository.findById(stopRequest.getAirportId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stop airport not found with id " + stopRequest.getAirportId()));
                stop.setAirport(airport);
                stop.setFlight(flight); // Set the flight reference
                // Calculate stop duration if arrival and departure times are provided
                if (stopRequest.getArrivalTime() != null && stopRequest.getDepartureTime() != null) {
                    stop.setStopDuration((int) Duration.between(stopRequest.getArrivalTime(), stopRequest.getDepartureTime()).toMinutes());
                }
                return stop;
            }).collect(Collectors.toList());
            flight.setStopsList(stops);
        }

        // Dynamic pricing: increase price if within 24h of departure
        if (Duration.between(LocalDateTime.now(), request.getDepartureTime()).toHours() <= 24) {
            BigDecimal increasedPrice = request.getBasePrice().multiply(BigDecimal.valueOf(1.2)); // 20% increase
            flight.setBasePrice(increasedPrice);
        }

        // Set availableSeats và duration tự động
        flight.setAvailableSeats(calculateAvailableSeats(flight));
        flight.setDuration((int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());

        Flight saved = flightRepository.save(flight);
        seatService.createSeatsForFlight(saved);
        log.info("Flight created with ID: {}", saved.getFlightId());
        return flightMapper.toResponseDTO(saved);
    }

    @Override
    public FlightResponse updateFlight(Long id, FlightRequest request) {
        log.info("Updating flight with ID: {}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id " + id));
        if (flight.getStatus() == FlightStatus.DEPARTED || flight.getStatus() == FlightStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update a departed or cancelled flight");
        }
        if (request.getDepartureTime().isAfter(request.getArrivalTime())) {
            throw new IllegalArgumentException("Departure time must be before arrival time");
        }
        // Xóa validation availableSeats vì sẽ tính tự động
        // if (request.getAvailableSeats() <= 0) {
        //     throw new IllegalArgumentException("Available seats must be greater than 0");
        // }
        // Check for overlapping flights (aircraft)
        if (flightRepository.existsByAircraftIdAndTimeOverlap(
                request.getAircraftId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException("Aircraft is already scheduled for another flight in this time range");
        }
        // Check for overlapping flights (gate)
        if (flightRepository.existsByGateIdAndTimeOverlap(
                request.getGateId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException("Gate is already assigned to another flight in this time range");
        }

        // Check for overlapping flights at departure airport
        if (isAirportDepartureOverlap(request.getDepartureAirportId(), request.getDepartureTime(), request.getArrivalTime(), id)) {
            throw new IllegalArgumentException("Departure airport is busy with another flight in this time range");
        }

        // Check for overlapping flights at arrival airport
        if (isAirportArrivalOverlap(request.getArrivalAirportId(), request.getDepartureTime(), request.getArrivalTime(), id)) {
            throw new IllegalArgumentException("Arrival airport is busy with another flight in this time range");
        }

        // Check buffer time at airports
        if (hasSufficientBufferTime(request.getDepartureAirportId(), request.getDepartureTime(), MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Insufficient buffer time at departure airport");
        }
        if (hasSufficientBufferTime(request.getArrivalAirportId(), request.getArrivalTime(), MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Insufficient buffer time at arrival airport");
        }

        // Update basic flight fields
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        // Tự động tính duration từ departureTime và arrivalTime
        if (request.getDepartureTime() != null && request.getArrivalTime() != null) {
            flight.setDuration((int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());
        }
        flight.setStops(request.getStops()); // Kept for backward compatibility
        // Xóa set availableSeats vì sẽ tính tự động
        // flight.setAvailableSeats(request.getAvailableSeats());
        flight.setAirline(airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id " + id)));
        flight.setDepartureAirport(airportRepository.findById(request.getDepartureAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure airport not found with id " + request.getDepartureAirportId())));
        flight.setArrivalAirport(airportRepository.findById(request.getArrivalAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Arrival airport not found with id " + request.getArrivalAirportId())));
        flight.setGate(gateRepository.findById(request.getGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found with id " + request.getGateId())));
        flight.setAircraft(aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found with id " + request.getAircraftId())));

        // Update flight travel classes if provided
        if (request.getFlightTravelClasses() != null) {
            if (request.getFlightTravelClasses().isEmpty()) {
                flight.getFlightTravelClasses().clear();
            } else {
                // Clear existing and add new ones
                flight.getFlightTravelClasses().clear();
                List<FlightTravelClass> flightTravelClasses = request.getFlightTravelClasses().stream()
                    .map(ftcRequest -> {
                        FlightTravelClass ftc = new FlightTravelClass();
                        ftc.setFlight(flight);
                        TravelClass travelClass = travelClassRepository.findById(ftcRequest.getClassId())
                            .orElseThrow(() -> new IllegalArgumentException("Travel class not found with id " + ftcRequest.getClassId()));
                        ftc.setTravelClass(travelClass);
                        ftc.setCustomPrice(ftcRequest.getCustomPrice());
                        ftc.setAvailableSeats(ftcRequest.getAvailableSeats());
                        return ftc;
                    })
                    .collect(Collectors.toList());
                flight.setFlightTravelClasses(flightTravelClasses);
            }
        }

        // Update stops
        // First, remove existing stops
        stopRepository.deleteAll(flight.getStopsList());
        flight.getStopsList().clear();
        // Add new stops
        if (request.getStopsList() != null && !request.getStopsList().isEmpty()) {
            // Validate stop duration
            for (StopRequest stop : request.getStopsList()) {
                if (stop.getArrivalTime() != null && stop.getDepartureTime() != null) {
                    long stopDuration = java.time.Duration.between(stop.getArrivalTime(), stop.getDepartureTime()).toMinutes();
                    if (stopDuration < 20) {
                        throw new IllegalArgumentException("Stop duration must be at least 20 minutes");
                    }
                }
            }

            List<Stop> stops = request.getStopsList().stream().map(stopRequest -> {
                Stop stop = stopMapper.toEntity(stopRequest);
                Airport airport = airportRepository.findById(stopRequest.getAirportId())
                        .orElseThrow(() -> new ResourceNotFoundException("Stop airport not found with id " + stopRequest.getAirportId()));
                stop.setAirport(airport);
                stop.setFlight(flight); // Set the flight reference
                // Calculate stop duration if arrival and departure times are provided
                if (stopRequest.getArrivalTime() != null && stopRequest.getDepartureTime() != null) {
                    stop.setStopDuration((int) Duration.between(stopRequest.getArrivalTime(), stopRequest.getDepartureTime()).toMinutes());
                }
                return stop;
            }).collect(Collectors.toList());
            flight.setStopsList(stops);
        }

        // Dynamic pricing: increase price if within 24h of departure
        BigDecimal basePrice = request.getBasePrice();
        if (Duration.between(LocalDateTime.now(), request.getDepartureTime()).toHours() <= 24) {
            basePrice = basePrice.multiply(BigDecimal.valueOf(1.2));
        }
        flight.setBasePrice(basePrice);

        // Set availableSeats và duration tự động
        flight.setAvailableSeats(calculateAvailableSeats(flight));
        flight.setDuration((int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());

        flight.setStatus(request.getStatus());
        Flight updated = flightRepository.save(flight);
        log.info("Flight updated with ID: {}", updated.getFlightId());
        return flightMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<FlightResponse> findById(Long id) {
        log.info("Finding flight by ID: {}", id);
        return flightRepository.findById(id).map(flightMapper::toResponseDTO);
    }

    @Override
    public PageResponse<FlightResponse> findAll(Pageable pageable) {
        log.info("Finding all flights with pagination: {}", pageable);
        Page<Flight> page = flightRepository.findAll(pageable);
        return new PageResponse<>(page.map(flightMapper::toResponseDTO));
    }

    @Override
    public PageResponse<FlightResponse> searchFlights(
            Long departureAirportId,
            Long arrivalAirportId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            FlightStatus status,
            Pageable pageable) {

        log.info("Searching flights with departureAirportId: {}, arrivalAirportId: {}, startTime: {}, endTime: {}, status: {}",
                departureAirportId, arrivalAirportId, startTime, endTime, status);

        // If no search parameters are provided, return all flights
        if (departureAirportId == null &&
                arrivalAirportId == null &&
                startTime == null &&
                endTime == null &&
                status == null) {
            return findAll(pageable);
        }

        // Otherwise, proceed with the filtered search
        Page<Flight> page = flightRepository.searchFlights(
                departureAirportId,
                arrivalAirportId,
                startTime,
                endTime,
                status,
                pageable);
        return new PageResponse<>(page.map(flightMapper::toResponseDTO));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting flight with ID: {}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id " + id));
        if (flight.getStatus() == FlightStatus.DEPARTED || flight.getStatus() == FlightStatus.CANCELLED) {
            throw new IllegalStateException("Cannot delete a departed or cancelled flight");
        }
        flightRepository.deleteById(id);
        log.info("Flight deleted: {}", id);
    }

    @Override
    public PageResponse<FlightResponse> findDomesticFlights(String country, Pageable pageable) {
        log.info("Finding domestic flights in {} with pageable: {}", country, pageable);

        Page<Flight> flightPage = flightRepository.findDomesticFlights(country, pageable);
        return new PageResponse<>(flightPage.map(flightMapper::toResponseDTO));
    }

    @Override
    public PageResponse<FlightResponse> findFlightsBetweenCountries(String departureCountry, String arrivalCountry, Pageable pageable) {
        log.info("Finding flights from {} to {} with pageable: {}", departureCountry, arrivalCountry, pageable);

        Page<Flight> flightPage = flightRepository.findFlightsBetweenCountries(departureCountry, arrivalCountry, pageable);
        return new PageResponse<>(flightPage.map(flightMapper::toResponseDTO));
    }

    @Override
    public RoundTripFlightResponse searchRoundTripFlights(
            Long departureAirportId,
            Long arrivalAirportId,
            LocalDateTime outboundDate,
            LocalDateTime returnDate,
            FlightStatus status,
            Pageable pageable) {
        LocalDateTime outboundStart = outboundDate.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime outboundEnd = outboundDate.withHour(23).withMinute(59).withSecond(59);
        LocalDateTime returnStart = returnDate.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime returnEnd = returnDate.withHour(23).withMinute(59).withSecond(59);

        Page<Flight> allRoundTripFlights = flightRepository.findAllRoundTripFlights(pageable);
        // Lọc outbound: đúng chiều, đúng ngày đi
        List<Flight> outboundFlights = allRoundTripFlights.getContent().stream()
            .filter(f -> f.getDepartureAirport().getAirportId().equals(departureAirportId)
                && f.getArrivalAirport().getAirportId().equals(arrivalAirportId)
                && !f.getDepartureTime().isBefore(outboundStart)
                && !f.getDepartureTime().isAfter(outboundEnd))
            .toList();
        // Lọc return: đúng chiều, đúng ngày về
        List<Flight> returnFlights = allRoundTripFlights.getContent().stream()
            .filter(f -> f.getDepartureAirport().getAirportId().equals(arrivalAirportId)
                && f.getArrivalAirport().getAirportId().equals(departureAirportId)
                && !f.getDepartureTime().isBefore(returnStart)
                && !f.getDepartureTime().isAfter(returnEnd))
            .toList();

        // Ghép cặp khứ hồi hợp lệ dựa trên roundTripGroupId
        List<RoundTripFlightResponse.RoundTripPair> pairs = outboundFlights.stream()
            .flatMap(outbound -> returnFlights.stream()
                .filter(inbound -> outbound.getRoundTripGroupId() != null
                        && outbound.getRoundTripGroupId().equals(inbound.getRoundTripGroupId()))
                .map(inbound -> new RoundTripFlightResponse.RoundTripPair(
                        flightMapper.toResponseDTO(outbound),
                        flightMapper.toResponseDTO(inbound)
                )))
            .toList();

        RoundTripFlightResponse response = new RoundTripFlightResponse();
        response.setRoundTripPairs(pairs);
        return response;
    }

    @Override
    public PageResponse<FlightResponse> findRoundTripFlightsByGroupId(String groupId, Pageable pageable) {
        Page<Flight> page = flightRepository.findRoundTripFlightsByGroupId(groupId, pageable);
        return new PageResponse<>(page.map(flightMapper::toResponseDTO));
    }
}
