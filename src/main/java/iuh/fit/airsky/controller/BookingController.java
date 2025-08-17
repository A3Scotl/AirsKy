package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.BookingRequest;
import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole( 'CUSTOMER')")
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
    @PreAuthorize("hasAnyRole('BUSINESS', 'CUSTOMER','ADMIN')")
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
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
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
}