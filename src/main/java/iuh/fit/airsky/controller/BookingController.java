package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.request.CheckinRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.CheckinEligiblePassengerResponse;
import iuh.fit.airsky.dto.response.CheckinResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
//    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        try {
            BookingResponse response = bookingService.createBooking(request);
            return ApiResponseUtil.buildResponse(true, "Booking created successfully", response, "/api/v1/bookings");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/bookings");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(@PathVariable Long id, @Valid @RequestBody BookingRequest request) {
        try {
            BookingResponse response = bookingService.updateBooking(id, request);
            return ApiResponseUtil.buildResponse(true, "Booking updated successfully", response, "/api/v1/bookings/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/bookings/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/bookings/" + id);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS', 'CUSTOMER', 'STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long id) {
        try {
            return bookingService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Booking retrieved successfully", response, "/api/v1/bookings/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Booking not found", "RESOURCE_NOT_FOUND", "/api/v1/bookings/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/bookings/" + id);
        }
    }

    @GetMapping
//    @PreAuthorize("hasAnyRole('ADMIN', 'BUSINESS', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getAllBookings(Pageable pageable) {
        try {
            PageResponse<BookingResponse> response = bookingService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Bookings retrieved successfully", response, "/api/v1/bookings");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/bookings");
        }
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBooking(@PathVariable Long id) {
        try {
            bookingService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Booking deleted successfully", null, "/api/v1/bookings/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/bookings/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/bookings/" + id);
        }
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<BookingResponse>> lookupBooking(
            @RequestParam String bookingCode,
            @RequestParam String fullName) {
        try {
            Optional<BookingResponse> bookingOpt = bookingService.findByBookingCodeAndPassengerName(bookingCode, fullName);
            if (bookingOpt.isPresent()) {
                return ApiResponseUtil.buildResponse(true, "Booking found", bookingOpt.get(), "/api/v1/bookings/lookup");
            } else {
                return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Booking not found", "BOOKING_NOT_FOUND", "/api/v1/bookings/lookup");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lookup failed", ex.getMessage(), "/api/v1/bookings/lookup");
        }
    }

    @GetMapping("/checkin-eligible")
    public ResponseEntity<ApiResponse<List<CheckinEligiblePassengerResponse>>> getCheckinEligiblePassengers(
            @RequestParam String bookingCode,
            @RequestParam String fullName) {
        try {
            List<CheckinEligiblePassengerResponse> passengers = bookingService.getCheckinEligiblePassengers(bookingCode, fullName);
            return ApiResponseUtil.buildResponse(true, "Check-in eligible passengers retrieved", passengers, "/api/v1/bookings/checkin-eligible");
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/bookings/checkin-eligible");
        } catch (IllegalStateException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_STATE", "/api/v1/bookings/checkin-eligible");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get check-in eligible passengers", ex.getMessage(), "/api/v1/bookings/checkin-eligible");
        }
    }

    @GetMapping("/passengers-checkin-status")
    public ResponseEntity<ApiResponse<List<CheckinEligiblePassengerResponse>>> getPassengersWithCheckinStatus(
            @RequestParam String bookingCode,
            @RequestParam String fullName) {
        try {
            List<CheckinEligiblePassengerResponse> passengers = bookingService.getPassengersWithCheckinStatus(bookingCode, fullName);
            return ApiResponseUtil.buildResponse(true, "Passengers with check-in status retrieved", passengers, "/api/v1/bookings/passengers-checkin-status");
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/bookings/passengers-checkin-status");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get passengers with check-in status", ex.getMessage(), "/api/v1/bookings/passengers-checkin-status");
        }
    }

    @PostMapping("/{bookingId}/guest-payment")
    public ResponseEntity<ApiResponse<BookingResponse>> processGuestPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        try {
            BookingResponse response = bookingService.processPaymentForGuestBooking(bookingId, paymentRequest);
            return ApiResponseUtil.buildResponse(true, "Payment processed successfully", response, "/api/v1/bookings/" + bookingId + "/guest-payment");
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/bookings/" + bookingId + "/guest-payment");
        } catch (IllegalStateException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_STATE", "/api/v1/bookings/" + bookingId + "/guest-payment");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Payment processing failed", ex.getMessage(), "/api/v1/bookings/" + bookingId + "/guest-payment");
        }
    }

    @PutMapping("/checkin")
    public ResponseEntity<ApiResponse<CheckinResponse>> processCheckin(@Valid @RequestBody CheckinRequest request) {
        try {
            CheckinResponse response = bookingService.processCheckin(request);
            return ApiResponseUtil.buildResponse(true, "Check-in processed successfully", response, "/api/v1/bookings/checkin");
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/bookings/checkin");
        } catch (IllegalStateException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_STATE", "/api/v1/bookings/checkin");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Check-in processing failed", ex.getMessage(), "/api/v1/bookings/checkin");
        }
    }
}