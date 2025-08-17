package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.GateRequest;
import iuh.fit.airsky.dto.response.GateResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface GateService {
    GateResponse createGate(GateRequest request);
    GateResponse updateGate(Long id, GateRequest request);
    Optional<GateResponse> findById(Long id);
    PageResponse<GateResponse> findAll(Pageable pageable);
    void softDelete(Long id);
}