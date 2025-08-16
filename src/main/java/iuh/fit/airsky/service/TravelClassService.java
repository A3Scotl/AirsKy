package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.TravelClassRequest;
import iuh.fit.airsky.dto.response.TravelClassResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TravelClassService {
    TravelClassResponse createTravelClass(TravelClassRequest request);
    TravelClassResponse updateTravelClass(Long id, TravelClassRequest request);
    Optional<TravelClassResponse> findById(Long id);
    PageResponse<TravelClassResponse> findAll(Pageable pageable);
    void delete(Long id);
}