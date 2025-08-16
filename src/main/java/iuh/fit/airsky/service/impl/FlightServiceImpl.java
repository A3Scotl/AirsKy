package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.FlightStatusType;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.FlightMapper;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.repository.AirlineRepository;
import iuh.fit.airsky.repository.AirportRepository;
import iuh.fit.airsky.repository.FlightRepository;
import iuh.fit.airsky.repository.GateRepository;
import iuh.fit.airsky.service.FlightService;
import iuh.fit.airsky.util.GenerateCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public FlightServiceImpl(FlightRepository flightRepository, FlightMapper flightMapper, AirlineRepository airlineRepository, AirportRepository airportRepository, GateRepository gateRepository, GenerateCodeUtil generateCodeUtil) {
        this.flightRepository = flightRepository;
        this.flightMapper = flightMapper;
        this.airlineRepository = airlineRepository;
        this.airportRepository = airportRepository;
        this.gateRepository = gateRepository;
        this.generateCodeUtil = generateCodeUtil;
    }

    @Override
    public FlightResponse createFlight(FlightRequest request) {
        log.info("Creating new flight");
        Flight flight = flightMapper.toEntity(request);
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
        Flight saved = flightRepository.save(flight);
        log.info("Flight created with ID: {}", saved.getFlightId());
        return flightMapper.toResponseDTO(saved);
    }

    @Override
    public FlightResponse updateFlight(Long id, FlightRequest request) {
        log.info("Updating flight with ID: {}", id);
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id " + id));
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setDuration(request.getDuration());
        flight.setStops(request.getStops());
        flight.setAvailableSeats(request.getAvailableSeats());
        flight.setBasePrice(request.getBasePrice());
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
    public PageResponse<FlightResponse> searchFlights(Long departureAirportId, Long arrivalAirportId, LocalDateTime startTime, LocalDateTime endTime, FlightStatusType status, Pageable pageable) {
        log.info("Searching flights with departureAirportId: {}, arrivalAirportId: {}, startTime: {}, endTime: {}, status: {}", departureAirportId, arrivalAirportId, startTime, endTime, status);
        Page<Flight> page = flightRepository.searchFlights(departureAirportId, arrivalAirportId, startTime, endTime, status, pageable);
        return new PageResponse<>(page.map(flightMapper::toResponseDTO));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting flight with ID: {}", id);
        if (!flightRepository.existsById(id)) {
            log.warn("Flight not found for delete: {}", id);
            throw new ResourceNotFoundException("Flight not found with id " + id);
        }
        flightRepository.deleteById(id);
        log.info("Flight deleted: {}", id);
    }
}