
package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.FlightMapper;
import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.Stop;
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

@Service
@Slf4j
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final GateRepository gateRepository;
    private final GenerateCodeUtil generateCodeUtil;
    private final SeatService seatService;
    private final AircraftRepository aircraftRepository;

    public FlightServiceImpl(FlightRepository flightRepository, FlightMapper flightMapper, AirlineRepository airlineRepository, AirportRepository airportRepository, GateRepository gateRepository, GenerateCodeUtil generateCodeUtil, SeatService seatService, AircraftRepository aircraftRepository) {
        this.flightRepository = flightRepository;
        this.flightMapper = flightMapper;
        this.airlineRepository = airlineRepository;
        this.airportRepository = airportRepository;
        this.gateRepository = gateRepository;
        this.generateCodeUtil = generateCodeUtil;
        this.seatService = seatService;
        this.aircraftRepository = aircraftRepository;
    }

    @Override
    public FlightResponse createFlight(FlightRequest request) {
        log.info("Creating new flight");

        // Validate input
        if (request.getDepartureTime().isAfter(request.getArrivalTime())) {
            throw new IllegalArgumentException("Departure time must be before arrival time");
        }
        if (request.getAvailableSeats() <= 0) {
            throw new IllegalArgumentException("Available seats must be greater than 0");
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

        Flight flight = flightMapper.toEntity(request);
        Aircraft aircraft = aircraftRepository.findById(request.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found"));
        flight.setAircraft(aircraft);

        flight.setAirline(airlineRepository.findById(request.getAirlineId())
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with id " + request.getAirlineId())));
        String airlineCode = flight.getAirline().getAirlineCode();
        flight.setFlightNumber(generateCodeUtil.generateFlightNumber(flightRepository, airlineCode));
        flight.setDepartureAirport(airportRepository.findById(request.getDepartureAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure airport not found with id " + request.getDepartureAirportId())));
        flight.setArrivalAirport(airportRepository.findById(request.getArrivalAirportId())
                .orElseThrow(() -> new ResourceNotFoundException("Arrival airport not found with id " + request.getArrivalAirportId())));
        flight.setGate(gateRepository.findById(request.getGateId())
                .orElseThrow(() -> new ResourceNotFoundException("Gate not found with id " + request.getGateId())));

        // Dynamic pricing: increase price if within 24h of departure
        if (Duration.between(LocalDateTime.now(), request.getDepartureTime()).toHours() <= 24) {
            BigDecimal increasedPrice = request.getBasePrice().multiply(BigDecimal.valueOf(1.2)); // 20% increase
            flight.setBasePrice(increasedPrice);
        }
        if (request.getStops() != null && !request.getStops().isEmpty()) {
            List<Stop> stopEntities = request.getStops().stream()
                    .map(req -> Stop.builder()
                            .flight(flight)
                            .airport(airportRepository.findById(req.getAirportId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id " + req.getAirportId())))
                            .arrivalTime(req.getArrivalTime())
                            .departureTime(req.getDepartureTime())
                            .note(req.getNote())
                            .build())
                    .toList();
            flight.setStops(stopEntities);
        }
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
        if (request.getAvailableSeats() <= 0) {
            throw new IllegalArgumentException("Available seats must be greater than 0");
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

        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setDuration(request.getDuration());
        if (request.getStops() != null && !request.getStops().isEmpty()) {
            List<Stop> stopEntities = request.getStops().stream()
                    .map(req -> Stop.builder()
                            .flight(flight)
                            .airport(airportRepository.findById(req.getAirportId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Airport not found with id " + req.getAirportId())))
                            .arrivalTime(req.getArrivalTime())
                            .departureTime(req.getDepartureTime())
                            .note(req.getNote())
                            .build())
                    .toList();
            flight.setStops(stopEntities);
        }
        flight.setAvailableSeats(request.getAvailableSeats());

        // Dynamic pricing: increase price if within 24h of departure
        BigDecimal basePrice = request.getBasePrice();
        if (Duration.between(LocalDateTime.now(), request.getDepartureTime()).toHours() <= 24) {
            basePrice = basePrice.multiply(BigDecimal.valueOf(1.2));
        }
        flight.setBasePrice(basePrice);

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
}