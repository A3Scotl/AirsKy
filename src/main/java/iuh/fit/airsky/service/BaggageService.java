package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.BaggageRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BaggageService {
    BaggageResponse createBaggage(BaggageRequest request);
//    BaggageResponse updateBaggage(Long id, BaggageRequest request);
    Optional<BaggageResponse> findById(Long id);
    PageResponse<BaggageResponse> findAll(Pageable pageable);
    void delete(Long id);
}