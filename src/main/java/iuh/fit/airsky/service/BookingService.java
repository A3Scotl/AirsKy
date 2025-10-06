package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request);
    BookingResponse updateBooking(Long id, BookingRequest request);
    Optional<BookingResponse> findById(Long id);
    PageResponse<BookingResponse> findAll(Pageable pageable);
    void delete(Long id);
    BookingResponse completeBooking(Long bookingId);
}