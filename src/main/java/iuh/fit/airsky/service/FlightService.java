package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.FlightStatusType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FlightService {
    FlightResponse createFlight(FlightRequest request);
    FlightResponse updateFlight(Long id, FlightRequest request);
    Optional<FlightResponse> findById(Long id);
    PageResponse<FlightResponse> findAll(Pageable pageable);
    PageResponse<FlightResponse> searchFlights(Long departureAirportId, Long arrivalAirportId, LocalDateTime startTime, LocalDateTime endTime, FlightStatusType status, Pageable pageable);
    void delete(Long id);
}