package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AirportService {
    AirportResponse createAirport(AirportRequest request);
    AirportResponse updateAirport(Long id, AirportRequest request);
    Optional<AirportResponse> findById(Long id);
    PageResponse<AirportResponse> findAll(Pageable pageable);
    void softDelete(Long id);
    Optional<AirportResponse> findByAirportCode(String airportCode);
}