package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.request.FlightSearchRequest;
import iuh.fit.airsky.dto.request.SearchSegment;
import iuh.fit.airsky.dto.request.StopRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.RoundTripFlightResponse;
import iuh.fit.airsky.dto.response.UnifiedFlightSearchResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.enums.TripType;
import iuh.fit.airsky.event.BookingCancelledEvent;
import iuh.fit.airsky.event.BookingConfirmedEvent;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.FlightMapper;
import iuh.fit.airsky.mapper.StopMapper;
import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Airport;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.FlightSegment;
import iuh.fit.airsky.model.FlightTravelClass;
import iuh.fit.airsky.model.Gate;
import iuh.fit.airsky.model.Payment;
import iuh.fit.airsky.model.Stop;
import iuh.fit.airsky.model.TravelClass;
import iuh.fit.airsky.repository.*;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.service.FlightService;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.service.PaymentService;
import iuh.fit.airsky.service.SeatService;
import iuh.fit.airsky.util.GenerateCodeUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FlightServiceImpl implements FlightService {

    private static final int MIN_BUFFER_TIME_MINUTES = 30;

    // Standard travel class names
    private static final String ECONOMY_CLASS = "Economy";
    private static final String BUSINESS_CLASS = "Business";
    private static final String FIRST_CLASS = "First";

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final FlightMapper flightMapper;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final GateRepository gateRepository;
    private final GenerateCodeUtil generateCodeUtil;
    private final AircraftRepository aircraftRepository;
    private final StopRepository stopRepository;
    private final StopMapper stopMapper;
    private final TravelClassRepository travelClassRepository;
    private final SeatService seatService;
    private final SeatRepository seatRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    public FlightServiceImpl(FlightRepository flightRepository, FlightMapper flightMapper,
            AirlineRepository airlineRepository, AirportRepository airportRepository,
            GateRepository gateRepository, GenerateCodeUtil generateCodeUtil,
            AircraftRepository aircraftRepository,
            StopRepository stopRepository, StopMapper stopMapper,
            TravelClassRepository travelClassRepository, SeatService seatService,
            SeatRepository seatRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            PaymentService paymentService,
            BookingService bookingService,
            ApplicationEventPublisher eventPublisher,
            NotificationService notificationService) {
        this.flightRepository = flightRepository;
        this.flightMapper = flightMapper;
        this.airlineRepository = airlineRepository;
        this.airportRepository = airportRepository;
        this.gateRepository = gateRepository;
        this.generateCodeUtil = generateCodeUtil;
        this.aircraftRepository = aircraftRepository;
        this.stopRepository = stopRepository;
        this.stopMapper = stopMapper;
        this.travelClassRepository = travelClassRepository;
        this.seatService = seatService;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
    }

    // Helper methods for schedule validation
    private boolean isAirportDepartureOverlap(Long airportId, LocalDateTime departureTime, LocalDateTime arrivalTime,
            Long excludeFlightId) {
        return flightRepository.existsByDepartureAirportIdAndTimeOverlap(airportId, departureTime, arrivalTime,
                excludeFlightId);
    }

    private boolean isAirportArrivalOverlap(Long airportId, LocalDateTime departureTime, LocalDateTime arrivalTime,
            Long excludeFlightId) {
        return flightRepository.existsByArrivalAirportIdAndTimeOverlap(airportId, departureTime, arrivalTime,
                excludeFlightId);
    }

    private boolean hasSufficientBufferTime(Long airportId, LocalDateTime time, int bufferMinutes) {
        LocalDateTime start = time.minusMinutes(bufferMinutes);
        LocalDateTime end = time.plusMinutes(bufferMinutes);
        return flightRepository.countFlightsAtAirportInTimeRange(airportId, start, end) > 0;
    }

    // Method helper để tính availableSeats tự động
    private Integer calculateAvailableSeats(Flight flight) {
        if (flight.getAircraft() == null) {
            log.warn("Aircraft is null for flight, using default capacity");
            return 180; // Default capacity
        }

        // Tính tổng ghế từ aircraft
        int totalSeats = flight.getAircraft().getTotalSeats();
        log.info("Aircraft {} has {} total seats", flight.getAircraft().getAircraftName(), totalSeats);

        // Tính ghế đã đặt (giả sử có BookingRepository)
        // int bookedSeats =
        // bookingRepository.countBookedSeatsByFlightId(flight.getFlightId());
        // return totalSeats - bookedSeats;

        // Tạm thời trả về totalSeats (có thể chỉnh sửa sau khi có BookingRepository)
        return totalSeats;
    }

    @Override
    public FlightResponse createFlight(FlightRequest request) {
        log.info("Creating new flight");

        // Validate input
        if (request.getDepartureTime().isAfter(request.getArrivalTime())) {
            throw new IllegalArgumentException("Thời gian khởi hành phải trước thời gian hạ cánh");
        }

        // Basic validation only - support all trip types but keep it simple

        // check airline id
        if (!airlineRepository.existsById(request.getAirlineId())) {
            throw new ResourceNotFoundException("Không tìm thấy hãng hàng không với id " + request.getAirlineId());
        }

        // Check for overlapping flights (aircraft)
        if (flightRepository.existsByAircraftIdAndTimeOverlap(
                request.getAircraftId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException(
                    "Máy bay đã được lên lịch cho chuyến bay khác trong khoảng thời gian này");
        }

        // Check for overlapping flights (gate)
        if (flightRepository.existsByGateIdAndTimeOverlap(
                request.getGateId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException("Cổng đã được gán cho chuyến bay khác trong khoảng thời gian này");
        }

        // Check for overlapping flights at departure airport
        if (isAirportDepartureOverlap(request.getDepartureAirportId(), request.getDepartureTime(),
                request.getArrivalTime(), null)) {
            throw new IllegalArgumentException(
                    "Sân bay khởi hành đang bận với chuyến bay khác trong khoảng thời gian này");
        }

        // Check for overlapping flights at arrival airport
        if (isAirportArrivalOverlap(request.getArrivalAirportId(), request.getDepartureTime(), request.getArrivalTime(),
                null)) {
            throw new IllegalArgumentException("Sân bay đến đang bận với chuyến bay khác trong khoảng thời gian này");
        }

        // Check buffer time at airports
        if (hasSufficientBufferTime(request.getDepartureAirportId(), request.getDepartureTime(),
                MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Không đủ thời gian chờ tại sân bay khởi hành");
        }
        if (hasSufficientBufferTime(request.getArrivalAirportId(), request.getArrivalTime(), MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Không đủ thời gian chờ tại sân bay đến");
        }

        // Validate gate belongs to departure airport
        Gate gate = gateRepository.findById(request.getGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cổng"));
        if (gate.getAirport() == null || !gate.getAirport().getAirportId().equals(request.getDepartureAirportId())) {
            throw new IllegalArgumentException("Cổng phải thuộc về sân bay khởi hành");
        }

        // Map DTO to entity
        Flight flight = flightMapper.toEntity(request);
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found"));
        flight.setAircraft(aircraft);

        // Set airline
        flight.setAirline(airlineRepository.findById(request.getAirlineId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hãng hàng không với id " + request.getAirlineId())));

        // Tự động tính duration từ departureTime và arrivalTime
        if (request.getDepartureTime() != null && request.getArrivalTime() != null) {
            flight.setDuration(
                    (int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());
        }

        String airlineCode = flight.getAirline().getAirlineCode();
        flight.setFlightNumber(generateCodeUtil.generateFlightNumber(flightRepository, airlineCode));
        flight.setDepartureAirport(airportRepository.findById(request.getDepartureAirportId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân bay khởi hành với id " + request.getDepartureAirportId())));
        flight.setArrivalAirport(airportRepository.findById(request.getArrivalAirportId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân bay đến với id " + request.getArrivalAirportId())));
        flight.setGate(gateRepository.findById(request.getGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found with id " + request.getGateId())));

        // Set base price from request
        if (request.getBasePrice() == null || request.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Base price must be provided and greater than 0");
        }
        flight.setBasePrice(request.getBasePrice());

        // Travel classes will be auto-generated by SQL trigger

        // Handle stops
        if (request.getStopsList() != null && !request.getStopsList().isEmpty()) {
            // Validate stop duration
            for (StopRequest stop : request.getStopsList()) {
                if (stop.getArrivalTime() != null && stop.getDepartureTime() != null) {
                    long stopDuration = java.time.Duration.between(stop.getArrivalTime(), stop.getDepartureTime())
                            .toMinutes();
                    if (stopDuration < 20) {
                        throw new IllegalArgumentException("Thời gian dừng phải ít nhất 20 phút");
                    }
                }
            }

            List<Stop> stops = request.getStopsList().stream().map(stopRequest -> {
                Stop stop = stopMapper.toEntity(stopRequest);
                Airport airport = airportRepository.findById(stopRequest.getAirportId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy sân bay dừng với id " + stopRequest.getAirportId()));
                stop.setAirport(airport);
                stop.setFlight(flight); // Set the flight reference
                // Calculate stop duration if arrival and departure times are provided
                if (stopRequest.getArrivalTime() != null && stopRequest.getDepartureTime() != null) {
                    stop.setStopDuration((int) Duration
                            .between(stopRequest.getArrivalTime(), stopRequest.getDepartureTime()).toMinutes());
                }
                return stop;
            }).collect(Collectors.toList());
            flight.setStopsList(stops);
        }

        // Dynamic pricing: increase price if within 24h of departure
        // Đã set basePrice ở trên, chỉ tăng giá nếu gần giờ bay
        // WARNING: This will increase basePrice by 20% if departure is within 24 hours!
        // Comment out the next 3 lines if you don't want dynamic pricing
        if (Duration.between(LocalDateTime.now(), request.getDepartureTime()).toHours() <= 24) {
            BigDecimal increasedPrice = flight.getBasePrice().multiply(BigDecimal.valueOf(1.2)); // 20% increase
            log.warn("Dynamic pricing applied: basePrice increased from {} to {} (departure within 24h)",
                    flight.getBasePrice(), increasedPrice);
            flight.setBasePrice(increasedPrice);
        }

        // Set availableSeats và duration tự động
        flight.setAvailableSeats(calculateAvailableSeats(flight));
        flight.setDuration((int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());

        Flight saved = flightRepository.save(flight);

        // Debug: Check basePrice after save
        log.info("Flight saved with ID: {}, basePrice: {}", saved.getFlightId(), saved.getBasePrice());

        // Generate standard travel classes after flight is saved
        List<FlightTravelClass> flightTravelClasses = generateStandardTravelClasses(saved);

        // Instead of replacing the collection, add each item to avoid orphan removal
        // issues
        for (FlightTravelClass ftc : flightTravelClasses) {
            saved.getFlightTravelClasses().add(ftc);
        }

        // Save again with travel classes
        Flight finalSaved = flightRepository.save(saved);
        log.info("Flight created with ID: {} - Travel classes auto-generated: {}",
                finalSaved.getFlightId(), flightTravelClasses.size());

        // Generate seats for each travel class after travel classes are saved
        seatService.createSeatsForFlight(finalSaved);
        log.info("Seats auto-generated for flight: {}", finalSaved.getFlightId());

        FlightResponse response = flightMapper.toResponseDTO(finalSaved);
        return enrichFlightResponseWithAvailableSeats(response);
    }

    /**
     * Enrich FlightResponse với availableSeats cho từng travel class
     */
    private FlightResponse enrichFlightResponseWithAvailableSeats(FlightResponse response) {
        if (response.getFlightTravelClasses() != null) {
            for (var ftc : response.getFlightTravelClasses()) {
                if (ftc.getTravelClass() != null && ftc.getTravelClass().getId() != null) {
                    Long availableCount = seatRepository.countAvailableSeatsByFlightIdAndTravelClassId(
                            response.getFlightId(),
                            ftc.getTravelClass().getId());
                    ftc.setAvailableSeats(availableCount.intValue());
                } else {
                    ftc.setAvailableSeats(0);
                }
            }
        }
        return response;
    }

    /**
     * Enrich list FlightResponse với availableSeats cho từng travel class
     */
    private List<FlightResponse> enrichFlightResponseListWithAvailableSeats(List<FlightResponse> responses) {
        if (responses != null) {
            responses.forEach(this::enrichFlightResponseWithAvailableSeats);
        }
        return responses;
    }

    @Override
    public FlightResponse updateFlight(Long id, FlightRequest request) {
        log.info("Updating flight with ID: {}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến bay với id " + id));
        if (flight.getStatus() == FlightStatus.DEPARTED || flight.getStatus() == FlightStatus.CANCELLED) {
            throw new IllegalStateException("Không thể cập nhật chuyến bay đã khởi hành hoặc đã hủy");
        }
        if (request.getDepartureTime().isAfter(request.getArrivalTime())) {
            throw new IllegalArgumentException("Thời gian khởi hành phải trước thời gian hạ cánh");
        }

        // Check for overlapping flights (aircraft)
        if (flightRepository.existsByAircraftIdAndTimeOverlap(
                request.getAircraftId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException(
                    "Máy bay đã được lên lịch cho chuyến bay khác trong khoảng thời gian này");
        }
        // Check for overlapping flights (gate)
        if (flightRepository.existsByGateIdAndTimeOverlap(
                request.getGateId(), request.getDepartureTime(), request.getArrivalTime())) {
            throw new IllegalArgumentException("Cổng đã được gán cho chuyến bay khác trong khoảng thời gian này");
        }

        // Check for overlapping flights at departure airport
        if (isAirportDepartureOverlap(request.getDepartureAirportId(), request.getDepartureTime(),
                request.getArrivalTime(), id)) {
            throw new IllegalArgumentException(
                    "Sân bay khởi hành đang bận với chuyến bay khác trong khoảng thời gian này");
        }

        // Check for overlapping flights at arrival airport
        if (isAirportArrivalOverlap(request.getArrivalAirportId(), request.getDepartureTime(), request.getArrivalTime(),
                id)) {
            throw new IllegalArgumentException("Sân bay đến đang bận với chuyến bay khác trong khoảng thời gian này");
        }

        // Check buffer time at airports
        if (hasSufficientBufferTime(request.getDepartureAirportId(), request.getDepartureTime(),
                MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Không đủ thời gian chờ tại sân bay khởi hành");
        }
        if (hasSufficientBufferTime(request.getArrivalAirportId(), request.getArrivalTime(), MIN_BUFFER_TIME_MINUTES)) {
            throw new IllegalArgumentException("Không đủ thời gian chờ tại sân bay đến");
        }

        // Update basic flight fields
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        // Tự động tính duration từ departureTime và arrivalTime
        if (request.getDepartureTime() != null && request.getArrivalTime() != null) {
            flight.setDuration(
                    (int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());
        }
        flight.setStops(request.getStops()); // Kept for backward compatibility
        // Xóa set availableSeats vì sẽ tính tự động
        // flight.setAvailableSeats(request.getAvailableSeats());
        flight.setAirline(airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id " + id)));
        flight.setDepartureAirport(airportRepository.findById(request.getDepartureAirportId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân bay khởi hành với id " + request.getDepartureAirportId())));
        flight.setArrivalAirport(airportRepository.findById(request.getArrivalAirportId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân bay đến với id " + request.getArrivalAirportId())));
        flight.setGate(gateRepository.findById(request.getGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found with id " + request.getGateId())));
        flight.setAircraft(aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Aircraft not found with id " + request.getAircraftId())));

        // Update base price if provided
        if (request.getBasePrice() != null) {
            if (request.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Base price must be greater than 0");
            }
            flight.setBasePrice(request.getBasePrice());
            // Travel classes will be updated by trigger or manual update
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
                    long stopDuration = java.time.Duration.between(stop.getArrivalTime(), stop.getDepartureTime())
                            .toMinutes();
                    if (stopDuration < 20) {
                        throw new IllegalArgumentException("Thời gian dừng phải ít nhất 20 phút");
                    }
                }
            }

            List<Stop> stops = request.getStopsList().stream().map(stopRequest -> {
                Stop stop = stopMapper.toEntity(stopRequest);
                Airport airport = airportRepository.findById(stopRequest.getAirportId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy sân bay dừng với id " + stopRequest.getAirportId()));
                stop.setAirport(airport);
                stop.setFlight(flight); // Set the flight reference
                // Calculate stop duration if arrival and departure times are provided
                if (stopRequest.getArrivalTime() != null && stopRequest.getDepartureTime() != null) {
                    stop.setStopDuration((int) Duration
                            .between(stopRequest.getArrivalTime(), stopRequest.getDepartureTime()).toMinutes());
                }
                return stop;
            }).collect(Collectors.toList());
            flight.setStopsList(stops);
        }

        // Dynamic pricing: increase price if within 24h of departure
        if (flight.getBasePrice() != null
                && Duration.between(LocalDateTime.now(), request.getDepartureTime()).toHours() <= 24) {
            BigDecimal basePrice = flight.getBasePrice();
            basePrice = basePrice.multiply(BigDecimal.valueOf(1.2));
            flight.setBasePrice(basePrice);
        }

        // Set availableSeats và duration tự động
        flight.setAvailableSeats(calculateAvailableSeats(flight));
        flight.setDuration((int) Duration.between(request.getDepartureTime(), request.getArrivalTime()).toMinutes());

        flight.setStatus(request.getStatus());
        Flight updated = flightRepository.save(flight);
        log.info("Flight updated with ID: {}", updated.getFlightId());
        FlightResponse response = flightMapper.toResponseDTO(updated);
        return enrichFlightResponseWithAvailableSeats(response);
    }

    @Override
    public Optional<FlightResponse> findById(Long id) {
        log.info("Finding flight by ID: {}", id);
        return flightRepository.findById(id)
                .map(flightMapper::toResponseDTO)
                .map(this::enrichFlightResponseWithAvailableSeats);
    }

    @Override
    public PageResponse<FlightResponse> findAll(Pageable pageable) {
        log.info("Finding all flights with pageable: {}", pageable);

        Page<Flight> flightPage = flightRepository.findAll(pageable);
        List<FlightResponse> flightResponses = flightPage.map(flightMapper::toResponseDTO).getContent();
        enrichFlightResponseListWithAvailableSeats(flightResponses);
        return new PageResponse<>(flightResponses, flightPage.getNumber(), flightPage.getSize(),
                flightPage.getTotalElements(), flightPage.getTotalPages(), flightPage.isLast());
    }

    @Override
    public PageResponse<FlightResponse> searchFlights(
            Long departureAirportId,
            Long arrivalAirportId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            FlightStatus status,
            String tripType,
            Pageable pageable) {

        log.info(
                "Searching flights with departureAirportId: {}, arrivalAirportId: {}, startTime: {}, endTime: {}, status: {}, tripType: {}",
                departureAirportId, arrivalAirportId, startTime, endTime, status, tripType);

        // Tối ưu: Luôn chỉ tìm các chuyến bay có thể đặt vé
        // 1. Trạng thái phải là ON_TIME
        status = FlightStatus.ON_TIME;
        // 2. Thời gian khởi hành phải sau thời điểm hiện tại ít nhất 4 tiếng
        LocalDateTime minDepartureTime = LocalDateTime.now().plusHours(4);
        if (startTime == null || startTime.isBefore(minDepartureTime)) {
            startTime = minDepartureTime;
        }

        // If no search parameters are provided, return all flights
        if (departureAirportId == null &&
                arrivalAirportId == null &&
                startTime == null &&
                endTime == null &&
                status == null &&
                tripType == null) {
            return findAll(pageable);
        }

        // Convert tripType String to TripType enum
        TripType tripTypeEnum = null;
        if (tripType != null && !tripType.isEmpty()) {
            try {
                tripTypeEnum = TripType.valueOf(tripType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid trip type: " + tripType);
            }
        }

        // Otherwise, proceed with the filtered search
        Page<Flight> page = flightRepository.searchFlights(
                departureAirportId,
                arrivalAirportId,
                startTime,
                endTime,
                status,
                tripTypeEnum,
                pageable);

        // Filter by travel class and available seats if travel class is specified
        // Note: This would be enhanced when travel class filtering is implemented
        List<FlightResponse> flightResponses = page.map(flightMapper::toResponseDTO).getContent();
        enrichFlightResponseListWithAvailableSeats(flightResponses);
        return new PageResponse<>(flightResponses, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
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
        List<FlightResponse> flightResponses = flightPage.map(flightMapper::toResponseDTO).getContent();
        enrichFlightResponseListWithAvailableSeats(flightResponses);
        return new PageResponse<>(flightResponses, flightPage.getNumber(), flightPage.getSize(),
                flightPage.getTotalElements(), flightPage.getTotalPages(), flightPage.isLast());
    }

    @Override
    public PageResponse<FlightResponse> findFlightsBetweenCountries(String departureCountry, String arrivalCountry,
            Pageable pageable) {
        log.info("Finding flights from {} to {} with pageable: {}", departureCountry, arrivalCountry, pageable);

        Page<Flight> flightPage = flightRepository.findFlightsBetweenCountries(departureCountry, arrivalCountry,
                pageable);
        List<FlightResponse> flightResponses = flightPage.map(flightMapper::toResponseDTO).getContent();
        enrichFlightResponseListWithAvailableSeats(flightResponses);
        return new PageResponse<>(flightResponses, flightPage.getNumber(), flightPage.getSize(),
                flightPage.getTotalElements(), flightPage.getTotalPages(), flightPage.isLast());
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

        // Search outbound flights using searchFlights with tripType filter
        PageResponse<FlightResponse> outboundFlightsPage = searchFlights(
                departureAirportId, arrivalAirportId, outboundStart, outboundEnd, status, "ROUND_TRIP", pageable);
        List<FlightResponse> outboundFlights = outboundFlightsPage.getContent();

        // Search return flights using searchFlights with tripType filter
        PageResponse<FlightResponse> returnFlightsPage = searchFlights(
                arrivalAirportId, departureAirportId, returnStart, returnEnd, status, "ROUND_TRIP", pageable);
        List<FlightResponse> returnFlights = returnFlightsPage.getContent();

        // Ghép cặp khứ hồi hợp lệ dựa trên roundTripGroupId
        List<RoundTripFlightResponse.RoundTripPair> pairs = outboundFlights.stream()
                .flatMap(outbound -> returnFlights.stream()
                        .filter(returnFlight -> outbound.getRoundTripGroupId() != null
                                && outbound.getRoundTripGroupId().equals(returnFlight.getRoundTripGroupId()))
                        .map(returnFlight -> new RoundTripFlightResponse.RoundTripPair(outbound, returnFlight)))
                .collect(Collectors.toList());

        RoundTripFlightResponse response = new RoundTripFlightResponse();
        response.setRoundTripPairs(pairs);
        return response;
    }

    @Override
    public PageResponse<FlightResponse> findRoundTripFlightsByGroupId(String groupId, Pageable pageable) {
        Page<Flight> page = flightRepository.findRoundTripFlightsByGroupId(groupId, pageable);
        List<FlightResponse> flightResponses = page.map(flightMapper::toResponseDTO).getContent();
        enrichFlightResponseListWithAvailableSeats(flightResponses);
        return new PageResponse<>(flightResponses, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    public UnifiedFlightSearchResponse searchUnifiedFlights(FlightSearchRequest request, Pageable pageable) {
        log.info("Searching unified flights for trip type: {}", request.getTripType());

        // Ensure tripType is parsed case-insensitively
        TripType tripTypeEnum = null;
        if (request.getTripType() != null) {
            if (request.getTripType() instanceof TripType) {
                tripTypeEnum = (TripType) request.getTripType();
            } else {
                try {
                    tripTypeEnum = TripType.fromString(request.getTripType().toString());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid trip type: " + request.getTripType());
                }
            }
        }
        if (tripTypeEnum == null) {
            throw new IllegalArgumentException("Trip type is required");
        }

        UnifiedFlightSearchResponse response = new UnifiedFlightSearchResponse();
        response.setTripType(tripTypeEnum);

        // Calculate total passengers for filtering
        int totalPassengers = (request.getAdultCount() != null ? request.getAdultCount() : 0) +
                (request.getChildCount() != null ? request.getChildCount() : 0);

        // Validate segments (populated by DTO validation)
        if (request.getSegments() == null || request.getSegments().isEmpty()) {
            throw new IllegalArgumentException("No valid segments provided for search");
        }

        // Process each segment uniformly
        List<PageResponse<FlightResponse>> segmentResults = new ArrayList<>();
        if (tripTypeEnum == TripType.MULTI_CITY) {
            SearchSegment firstSegment = request.getSegments().get(0);
            SearchSegment lastSegment = request.getSegments().get(request.getSegments().size() - 1);
            LocalDateTime startTime = firstSegment.getDepartureDate().atStartOfDay();
            LocalDateTime endTime = firstSegment.getDepartureDate().atTime(23, 59, 59);
            PageResponse<FlightResponse> multiCityResult = searchFlights(
                    firstSegment.getDepartureAirportId(),
                    lastSegment.getArrivalAirportId(),
                    startTime,
                    endTime,
                    null, // Status filter
                    tripTypeEnum.toValue(),
                    pageable);
            log.debug("[MULTI_CITY] Flights before stops filter: {}", multiCityResult.getContent().size());
            List<FlightResponse> filteredFlights = filterFlightsByStops(multiCityResult.getContent(),
                    request.getSegments());
            log.debug("[MULTI_CITY] Flights after stops filter: {}", filteredFlights.size());
            filteredFlights = filterFlightsByTravelClass(filteredFlights, request.getTravelClass(), totalPassengers);
            log.debug("[MULTI_CITY] Flights after travel class filter: {}", filteredFlights.size());
            multiCityResult.setContent(filteredFlights);
            segmentResults.add(multiCityResult);
        } else {
            for (SearchSegment segment : request.getSegments()) {
                LocalDateTime startTime = segment.getDepartureDate().atStartOfDay();
                LocalDateTime endTime = segment.getDepartureDate().atTime(23, 59, 59);
                PageResponse<FlightResponse> legResult = searchFlights(
                        segment.getDepartureAirportId(),
                        segment.getArrivalAirportId(),
                        startTime,
                        endTime,
                        null, // Status filter (optional)
                        tripTypeEnum.toValue(),
                        pageable);
                log.debug("[ONE_WAY/ROUND_TRIP] Flights before travel class filter: {}", legResult.getContent().size());
                List<FlightResponse> filteredFlights = filterFlightsByTravelClass(
                        legResult.getContent(), request.getTravelClass(), totalPassengers);
                log.debug("[ONE_WAY/ROUND_TRIP] Flights after travel class filter: {}", filteredFlights.size());
                legResult.setContent(filteredFlights);
                segmentResults.add(legResult);
            }
        }

        // Handle response based on trip type
        switch (tripTypeEnum) {
            case ONE_WAY:
                response.setOneWayFlights(segmentResults.get(0));
                break;
            case ROUND_TRIP:
                RoundTripFlightResponse roundTripResult = new RoundTripFlightResponse();
                if (segmentResults.size() >= 2) {
                    List<RoundTripFlightResponse.RoundTripPair> pairs = new ArrayList<>();
                    List<FlightResponse> outboundFlights = segmentResults.get(0).getContent();
                    List<FlightResponse> returnFlights = segmentResults.get(1).getContent();

                    // Pair flights (optimize by checking groupId or simple combination)
                    for (FlightResponse outbound : outboundFlights) {
                        for (FlightResponse returnFlight : returnFlights) {
                            // Optionally check roundTripGroupId if your DB supports it
                            if (outbound.getRoundTripGroupId() != null &&
                                    outbound.getRoundTripGroupId().equals(returnFlight.getRoundTripGroupId())) {
                                pairs.add(new RoundTripFlightResponse.RoundTripPair(outbound, returnFlight));
                            } else {
                                // Simple pairing if no groupId (all valid combinations)
                                pairs.add(new RoundTripFlightResponse.RoundTripPair(outbound, returnFlight));
                            }
                        }
                    }
                    roundTripResult.setRoundTripPairs(pairs);
                }
                response.setRoundTripPairs(roundTripResult.getRoundTripPairs());
                break;
            case MULTI_CITY:
                response.setMultiCityFlights(segmentResults);
                break;
            default:
                throw new IllegalArgumentException("Unsupported trip type: " + tripTypeEnum);
        }

        return response;
    }

    // Optimized filter method to avoid redundant DB queries
    private List<FlightResponse> filterFlightsByTravelClass(List<FlightResponse> flights, String travelClass,
            int totalPassengers) {
        log.debug("Filtering by travel class: {}, totalPassengers: {}, input flights: {}", travelClass, totalPassengers,
                flights != null ? flights.size() : 0);
        if (flights != null) {
            for (FlightResponse flight : flights) {
                if (flight.getFlightTravelClasses() != null) {
                    for (var ftc : flight.getFlightTravelClasses()) {
                        log.debug("FlightId: {}, Class: {}, Seats: {}", flight.getFlightId(),
                                ftc.getTravelClass() != null ? ftc.getTravelClass().getClassName() : null,
                                ftc.getCapacity());
                    }
                } else {
                    log.debug("FlightId: {} has no travel classes", flight.getFlightId());
                }
            }
        }
        if (travelClass == null || travelClass.isEmpty() || totalPassengers <= 0) {
            return flights;
        }
        List<FlightResponse> result = flights.stream()
                .filter(flight -> flight.getFlightTravelClasses() != null && flight.getFlightTravelClasses().stream()
                        .anyMatch(ftc -> ftc.getTravelClass().getClassName() != null &&
                                ftc.getTravelClass().getClassName().toLowerCase().contains(travelClass.toLowerCase()) &&
                                (ftc.getCapacity() - ftc.getBookedSeat()) >= totalPassengers))
                .collect(Collectors.toList());
        log.debug("After travel class filter: {}", result.size());
        return result;
    }

    private List<FlightResponse> filterFlightsByStops(List<FlightResponse> flights, List<SearchSegment> segments) {
        if (segments.size() < 2) {
            return flights; // No intermediate stops to check
        }

        // Get intermediate airports from segments (exclude first departure and last
        // arrival)
        List<Long> intermediateAirports = segments.subList(1, segments.size() - 1).stream()
                .map(SearchSegment::getDepartureAirportId)
                .collect(Collectors.toList());

        return flights.stream()
                .filter(flight -> {
                    if (flight.getStopsList() == null || flight.getStopsList().isEmpty()) {
                        return false;
                    }
                    // Check if all intermediate airports are in the stops list
                    List<Long> stopAirportIds = flight.getStopsList().stream()
                            .map(stop -> stop.getAirportId())
                            .collect(Collectors.toList());
                    return stopAirportIds.containsAll(intermediateAirports);
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<FlightResponse>> checkScheduleConflicts(
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            Long departureAirportId,
            Long arrivalAirportId,
            Long aircraftId,
            Long gateId,
            Long excludeFlightId) {
        Map<String, List<FlightResponse>> result = new java.util.HashMap<>();
        if (aircraftId != null && departureTime != null && arrivalTime != null) {
            List<Flight> aircraftConflicts = flightRepository.findAircraftConflicts(aircraftId, departureTime,
                    arrivalTime, excludeFlightId == null ? -1L : excludeFlightId);
            result.put("aircraft", aircraftConflicts.stream().map(flightMapper::toResponseDTO).toList());
        }
        if (gateId != null && departureTime != null && arrivalTime != null) {
            List<Flight> gateConflicts = flightRepository.findGateConflicts(gateId, departureTime, arrivalTime,
                    excludeFlightId == null ? -1L : excludeFlightId);
            result.put("gate", gateConflicts.stream().map(flightMapper::toResponseDTO).toList());
        }
        if (departureAirportId != null && departureTime != null && arrivalTime != null) {
            List<Flight> depConflicts = flightRepository.findDepartureAirportConflicts(departureAirportId,
                    departureTime, arrivalTime, excludeFlightId == null ? -1L : excludeFlightId);
            result.put("departureAirport", depConflicts.stream().map(flightMapper::toResponseDTO).toList());
        }
        if (arrivalAirportId != null && departureTime != null && arrivalTime != null) {
            List<Flight> arrConflicts = flightRepository.findArrivalAirportConflicts(arrivalAirportId, departureTime,
                    arrivalTime, excludeFlightId == null ? -1L : excludeFlightId);
            result.put("arrivalAirport", arrConflicts.stream().map(flightMapper::toResponseDTO).toList());
        }
        return result;
    }

    @Override
    public Map<String, Object> compareFlightPrices(Map<String, Object> params) {
        // Validate and parse params
        List<Map<String, Object>> routes = (List<Map<String, Object>>) params.get("routes");
        Integer dateRangeDays = (Integer) params.getOrDefault("dateRangeDays", 0);
        if (dateRangeDays == null)
            dateRangeDays = 0;
        if (dateRangeDays > 7)
            dateRangeDays = 7; // Giới hạn tối đa 7 ngày
        if (routes == null || routes.isEmpty())
            throw new IllegalArgumentException("Thiếu thông tin tuyến bay");

        Map<String, Object> result = new java.util.HashMap<>();
        List<Map<String, Object>> priceList = new ArrayList<>();

        // Lấy thời điểm hiện tại + 4 tiếng
        java.time.LocalDateTime nowPlus4h = java.time.LocalDateTime.now().plusHours(4);

        // One-way hoặc mặc định: chỉ lấy giá thấp nhất cho các chuyến bay khởi hành sau
        // 4 tiếng
        List<Long> departureAirportIds = new ArrayList<>();
        List<Long> arrivalAirportIds = new ArrayList<>();
        List<java.time.LocalDate> allDates = new ArrayList<>();
        for (Map<String, Object> route : routes) {
            Long depId = ((Number) route.get("departureAirportId")).longValue();
            Long arrId = ((Number) route.get("arrivalAirportId")).longValue();
            String dateStr = (String) route.get("date");
            java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
            departureAirportIds.add(depId);
            arrivalAirportIds.add(arrId);
            for (int i = -dateRangeDays; i <= dateRangeDays; i++) {
                allDates.add(date.plusDays(i));
            }
        }
        allDates = allDates.stream().distinct().toList();
        List<Object[]> minPrices = flightRepository.findMinPriceByRouteAndDatesWithMinDepartureTime(
                departureAirportIds, arrivalAirportIds, allDates, nowPlus4h);
        for (Object[] row : minPrices) {
            Map<String, Object> priceInfo = new java.util.HashMap<>();
            priceInfo.put("departureAirportId", row[0]);
            priceInfo.put("arrivalAirportId", row[1]);
            priceInfo.put("date", row[2].toString());
            priceInfo.put("minPrice", row[3]);
            priceList.add(priceInfo);
        }
        result.put("prices", priceList);
        result.put("type", "one-way");
        return result;
    }

    /**
     * Generate standard 3 travel classes for a flight
     * Economy (1.0x), Business (2.0x), First (3.0x)
     */
    private List<FlightTravelClass> generateStandardTravelClasses(Flight flight) {
        List<FlightTravelClass> flightTravelClasses = new ArrayList<>();

        // Get ONLY the 3 standard travel classes by name
        TravelClass economyClass = travelClassRepository.findByClassName(ECONOMY_CLASS)
                .orElseThrow(() -> new IllegalStateException(
                        "Economy travel class not found. Please run setup_standard_travel_classes.sql"));
        TravelClass businessClass = travelClassRepository.findByClassName(BUSINESS_CLASS)
                .orElseThrow(() -> new IllegalStateException(
                        "Business travel class not found. Please run setup_standard_travel_classes.sql"));
        TravelClass firstClass = travelClassRepository.findByClassName(FIRST_CLASS)
                .orElseThrow(() -> new IllegalStateException(
                        "First travel class not found. Please run setup_standard_travel_classes.sql"));

        // Validate price multipliers are not null
        if (economyClass.getPriceMultiplier() == null) {
            throw new IllegalStateException(
                    "Economy travel class price multiplier is null. Please run setup_standard_travel_classes.sql");
        }
        if (businessClass.getPriceMultiplier() == null) {
            throw new IllegalStateException(
                    "Business travel class price multiplier is null. Please run setup_standard_travel_classes.sql");
        }
        if (firstClass.getPriceMultiplier() == null) {
            throw new IllegalStateException(
                    "First travel class price multiplier is null. Please run setup_standard_travel_classes.sql");
        }

        // Calculate total seats and distribution from AIRCRAFT capacity
        Integer totalSeats = flight.getAircraft().getTotalSeats();
        if (totalSeats == null || totalSeats <= 0) {
            totalSeats = 180; // Default aircraft capacity
        }
        log.info("Using aircraft capacity: {} seats for flight travel class distribution", totalSeats);

        // Fixed seat distribution: Economy(120), Business(36), First(24) for 180 seats
        // Scale proportionally if aircraft has different capacity
        double scaleFactor = totalSeats / 180.0;
        int economySeats = (int) Math.round(120 * scaleFactor);
        int businessSeats = (int) Math.round(36 * scaleFactor);
        int firstSeats = totalSeats - economySeats - businessSeats; // Ensure total matches

        log.info("Seat distribution for {} total seats: Economy={}, Business={}, First={}",
                totalSeats, economySeats, businessSeats, firstSeats);

        // Create Economy class
        FlightTravelClass economyFTC = new FlightTravelClass();
        economyFTC.setFlight(flight);
        economyFTC.setTravelClass(economyClass);

        // Debug: Check basePrice before multiply
        BigDecimal basePrice = flight.getBasePrice();
        if (basePrice == null) {
            throw new IllegalStateException(
                    "Flight basePrice is null when generating travel classes. Flight ID: " + flight.getFlightId());
        }
        BigDecimal economyPrice = basePrice.multiply(economyClass.getPriceMultiplier());
        economyFTC.setPrice(economyPrice);
        economyFTC.setCapacity(economySeats);
        economyFTC.setBookedSeat(0);
        flightTravelClasses.add(economyFTC);

        // Create Business class
        FlightTravelClass businessFTC = new FlightTravelClass();
        businessFTC.setFlight(flight);
        businessFTC.setTravelClass(businessClass);
        BigDecimal businessPrice = basePrice.multiply(businessClass.getPriceMultiplier());
        businessFTC.setPrice(businessPrice);
        businessFTC.setCapacity(businessSeats);
        businessFTC.setBookedSeat(0);
        flightTravelClasses.add(businessFTC);

        // Create First class
        FlightTravelClass firstFTC = new FlightTravelClass();
        firstFTC.setFlight(flight);
        firstFTC.setTravelClass(firstClass);
        BigDecimal firstPrice = basePrice.multiply(firstClass.getPriceMultiplier());
        firstFTC.setPrice(firstPrice);
        firstFTC.setCapacity(firstSeats);
        firstFTC.setBookedSeat(0);
        flightTravelClasses.add(firstFTC);

        log.info("Generated 3 standard travel classes for flight: Economy, Business, First");
        return flightTravelClasses;
    }

    @Override
    @Transactional
    public FlightResponse cancelFlight(Long flightId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do hủy là bắt buộc (ví dụ: 'Thời tiết xấu')");
        }
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        if (flight.getStatus() == FlightStatus.DEPARTED) {
            throw new IllegalStateException("Không thể hủy chuyến bay đã khởi hành");
        }
        // Backup old status/time
        flight.setStatus(FlightStatus.CANCELLED);
        flight.setCanceledReason(reason);
        flight.setUpdatedAt(LocalDateTime.now());

        Flight savedFlight = flightRepository.save(flight);

        // Cascade: Cancel related bookings và refund
        List<Booking> bookings = bookingRepository.findByFlightAndStatus(flight, BookingStatus.CONFIRMED);
        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancellationReason("Chuyến bay " + flight.getFlightNumber() + " bị hủy: " + reason);
            bookingRepository.save(booking);

            Payment payment = booking.getPayment();
            if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
                // Full refund (100% cho cancel)
                BigDecimal refundAmt = booking.getTotalAmount(); // Hoặc trừ phí hủy nếu config
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundAmount(refundAmt);
                payment.setRefundDate(LocalDateTime.now());
                payment.setRefundReason("Hủy chuyến bay: " + reason);
                paymentRepository.save(payment);

                // Gửi email cho từng passenger (sử dụng payerBankName trong nội dung)
                if (booking.getUserId() != null || !booking.getPassengers().isEmpty()) {
                    eventPublisher.publishEvent(new BookingCancelledEvent(this, booking, reason));
                }
                // bookingService.refundPointsForCancelledBooking(booking);
            }
        }

        log.info("Flight {} ({} ) cancelled: Reason={}, Affected bookings: {}",
                flightId, flight.getFlightNumber(), reason, bookings.size());
        return flightMapper.toResponseDTO(savedFlight);
    }

    @Override
    @Transactional
    public FlightResponse delayFlight(Long flightId, String reason, LocalDateTime newDepartureTime) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do delay là bắt buộc (ví dụ: 'Thời tiết xấu')");
        }
        if (newDepartureTime == null || newDepartureTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian khởi hành mới phải sau thời điểm hiện tại");
        }
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        if (flight.getStatus() == FlightStatus.DEPARTED || flight.getStatus() == FlightStatus.CANCELLED) {
            throw new IllegalStateException("Không thể delay chuyến bay đã khởi hành hoặc đã hủy");
        }

        // Tính delay duration và cập nhật arrivalTime tương ứng (giả sử giữ nguyên
        // flight duration)
        long delayMinutes = java.time.Duration.between(flight.getDepartureTime(), newDepartureTime).toMinutes();
        if (delayMinutes <= 0) {
            throw new IllegalArgumentException("Thời gian delay phải lớn hơn thời gian hiện tại");
        }
        LocalDateTime newArrivalTime = flight.getArrivalTime().plusMinutes(delayMinutes);

        // Update flight
        flight.setStatus(FlightStatus.DELAYED);
        flight.setDelayedReason(reason); // Thêm field String delayedReason vào Flight model nếu chưa (tương tự
                                         // canceledReason)
        flight.setDepartureTime(newDepartureTime);
        flight.setArrivalTime(newArrivalTime);
        flight.setUpdatedAt(LocalDateTime.now());

        Flight savedFlight = flightRepository.save(flight);

        // Cascade: Update related bookings (không hủy, chỉ update time và notify)
        List<Booking> bookings = bookingRepository.findByFlightAndStatus(savedFlight, BookingStatus.CONFIRMED);
        for (Booking booking : bookings) {
            // Update booking departure/arrival time (nếu booking có flightSegments, update
            // tương ứng)
            if (booking.getFlightSegments() != null) {
                for (FlightSegment segment : booking.getFlightSegments()) {
                    if (segment.getFlight().getFlightId().equals(savedFlight.getFlightId())) {
                        segment.setDepartureTime(newDepartureTime);
                        segment.setArrivalTime(newArrivalTime);
                    }
                }
            } else if (booking.getFlight() != null
                    && booking.getFlight().getFlightId().equals(savedFlight.getFlightId())) {
                // Fallback cho single flight
                booking.getFlight().setDepartureTime(newDepartureTime);
                booking.getFlight().setArrivalTime(newArrivalTime);
            }
            // Thêm field nếu cần: booking.setDelayReason(reason); // Để lưu lý do cho
            // booking
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            // // Notify: Gửi email cho từng passenger (tương tự cancel, nhưng cho delay)
            // if (booking.getUserId() != null || !booking.getPassengers().isEmpty()) {
            //     String delayInfo = String.format("Chuyến bay %s bị delay %d phút, khởi hành mới lúc %s.",
            //             savedFlight.getFlightNumber(), delayMinutes,
            //             newDepartureTime.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))); // Ví dụ: 05/11/2025
            //     emailService.sendDelayEmail(booking, reason, delayInfo); // Giả sử method này tồn tại, tương tự
            //                                                              // sendCancellationEmail
            //     // Hoặc publish event mới: eventPublisher.publishEvent(new
            //     // BookingDelayedEvent(this, booking, reason, delayInfo));
            // }
            // Không hoàn points/deals vì không hủy
        }

        // Log và return RA NGOÀI LOOP (sửa lỗi return early)
        log.info(
                "Flight {} ({}) delayed on {}: Reason={}, New Departure={}, Delay Minutes={}, Affected bookings: {}",
                flightId, savedFlight.getFlightNumber(), LocalDateTime.now(), reason, newDepartureTime,
                delayMinutes, bookings.size()); // Di chuyển ra ngoài loop
        return flightMapper.toResponseDTO(savedFlight); // Di chuyển ra ngoài loop
    }
}
