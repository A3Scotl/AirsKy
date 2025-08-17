package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.AirlineRequest;
import iuh.fit.airsky.dto.response.AirlineResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AirlineService {
    AirlineResponse createAirline(AirlineRequest request);
    AirlineResponse updateAirline(Long id, AirlineRequest request);
    Optional<AirlineResponse> findById(Long id);
    PageResponse<AirlineResponse> findAll(Pageable pageable);
    void softDelete(Long id);
}