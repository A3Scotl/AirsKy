package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CheckinService {
    CheckinResponse createCheckin(CheckinRequest request);
    CheckinResponse updateCheckin(Long id, CheckinRequest request);
    Optional<CheckinResponse> findById(Long id);
    PageResponse<CheckinResponse> findAll(Pageable pageable);
    void softDelete(Long id);
}