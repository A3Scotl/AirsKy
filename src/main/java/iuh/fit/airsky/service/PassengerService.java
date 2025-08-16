package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.PassengerRequest;
import iuh.fit.airsky.dto.response.PassengerResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PassengerService {
    PassengerResponse createPassenger(PassengerRequest request);
    PassengerResponse updatePassenger(Long id, PassengerRequest request);
    Optional<PassengerResponse> findById(Long id);
    PageResponse<PassengerResponse> findAll(Pageable pageable);
    void delete(Long id);
}