package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.PassengerRequest;
import iuh.fit.airsky.dto.request.PassengerSeatRequest;
import iuh.fit.airsky.dto.response.PassengerResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.PassengerSeatResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PassengerService {
    PassengerResponse createPassenger(PassengerSeatRequest request);
    PassengerResponse updatePassenger(Long id, PassengerSeatRequest request);
    Optional<PassengerResponse> findById(Long id);
    PageResponse<PassengerResponse> findAll(Pageable pageable);
    void delete(Long id);
    List<PassengerSeatResponse> getPassengersWithSeats(Long bookingId);
}