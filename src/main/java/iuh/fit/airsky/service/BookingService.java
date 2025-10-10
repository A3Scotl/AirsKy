package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.CheckinEligiblePassengerResponse;
import iuh.fit.airsky.dto.response.CheckinResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request);
    BookingResponse updateBooking(Long id, BookingRequest request);
    Optional<BookingResponse> findById(Long id);
    PageResponse<BookingResponse> findAll(Pageable pageable);
    void delete(Long id);
    BookingResponse completeBooking(Long bookingId);
    Optional<BookingResponse> findByBookingCodeAndPassengerName(String bookingCode, String fullName);
    BookingResponse processPaymentForGuestBooking(Long bookingId, PaymentRequest paymentRequest);
    List<CheckinEligiblePassengerResponse> getCheckinEligiblePassengers(String bookingCode, String fullName);
    List<CheckinEligiblePassengerResponse> getPassengersWithCheckinStatus(String bookingCode, String fullName);
    CheckinResponse processCheckin(CheckinRequest request);
}