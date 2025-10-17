// PaymentController.java
package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.PaymentProcessingException;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.service.PaymentService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @Valid @RequestBody PaymentRequest request) {

        log.info("Received payment request for booking ID: {}", request.getBookingId());

        try {
            PaymentResponse response = paymentService.createPayment(request);
            return ApiResponseUtil.buildResponse(
                    true,
                    "Payment initiated successfully",
                    response,
                    "/api/v1/payments"
            );
        } catch (ResourceNotFoundException ex) {
            log.error("Resource not found: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    "Booking not found",
                    "RESOURCE_NOT_FOUND",
                    "/api/v1/payments"
            );
        } catch (PaymentProcessingException ex) {
            log.error("Payment processing failed: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage(),
                    "PAYMENT_PROCESSING_ERROR",
                    "/api/v1/payments"
            );
        }
    }

    @GetMapping("/success")
    public ResponseEntity<ApiResponse<PaymentResponse>> paymentSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @RequestParam("bookingId") Long bookingId
          ) {

        log.info("Processing successful payment - PaymentID: {}, BookingID: {}", paymentId, bookingId);

        try {
            PaymentResponse response = paymentService.executePayPalPayment(paymentId, payerId, bookingId);
            return ApiResponseUtil.buildResponse(
                    true,
                    "Payment completed successfully",
                    response,
                    "/api/v1/payments/success"
            );
        } catch (ResourceNotFoundException ex) {
            log.error("Payment not found: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.NOT_FOUND,
                    "Payment not found",
                    "PAYMENT_NOT_FOUND",
                    "/api/v1/payments/success"
            );
        } catch (PaymentProcessingException ex) {
            log.error("Payment execution failed: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage(),
                    "PAYMENT_EXECUTION_ERROR",
                    "/api/v1/payments/success"
            );
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> paymentCancel(
            @RequestParam("bookingId") Long bookingId
          ) {

        log.info("Payment cancelled for booking ID: {}", bookingId);

        try {
            return ApiResponseUtil.buildResponse(
                    true,
                    "Payment was cancelled",
                    null,
                    "/api/v1/payments/cancel"
            );
        } catch (Exception ex) {
            log.error("Error handling payment cancellation: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing payment cancellation",
                    "PAYMENT_CANCELLATION_ERROR",
                    "/api/v1/payments/cancel"
            );
        }
    }
    @GetMapping("/sepay/check/{bookingCode}")
    public ResponseEntity<Map<String, Object>> checkSepayPayment(@PathVariable String bookingCode) {
        log.info("Checking SePay transaction for bookingCode: {}", bookingCode);
        try {
            boolean success = paymentService.checkSepayTransaction(bookingCode);
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Payment found and updated successfully"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No transaction found for this booking code"
                ));
            }
        } catch (Exception e) {
            log.error("Error checking SePay transaction: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error checking SePay transaction"
            ));
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @PathVariable Long id) {

        log.info("Fetching payment with ID: {}", id);

        try {
            return paymentService.findById(id)
                    .map(payment -> ApiResponseUtil.buildResponse(
                            true,
                            "Payment retrieved successfully",
                            payment,
                            "/api/v1/payments/" + id
                    ))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(
                            HttpStatus.NOT_FOUND,
                            "Payment not found",
                            "PAYMENT_NOT_FOUND",
                            "/api/v1/payments/" + id
                    ));
        } catch (Exception ex) {
            log.error("Error fetching payment: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve payment",
                    "INTERNAL_SERVER_ERROR",
                    "/api/v1/payments/" + id
            );
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByBooking(
            @PathVariable Long bookingId,
            @RequestHeader("Authorization") String token) {

        log.info("Fetching payments for booking ID: {}", bookingId);

        try {
            if (!bookingRepository.existsById(bookingId)) {
                return ApiResponseUtil.buildErrorResponse(
                        HttpStatus.NOT_FOUND,
                        "Booking not found",
                        "BOOKING_NOT_FOUND",
                        "/api/v1/payments/booking/" + bookingId
                );
            }

            List<PaymentResponse> payments = paymentService.findByBookingId(bookingId);
            return ApiResponseUtil.buildResponse(
                    true,
                    payments.isEmpty() ? "No payments found" : "Payments retrieved successfully",
                    payments,
                    "/api/v1/payments/booking/" + bookingId
            );
        } catch (Exception ex) {
            log.error("Error fetching payments: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve payments",
                    "INTERNAL_SERVER_ERROR",
                    "/api/v1/payments/booking/" + bookingId
            );
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getAllPayments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        try {
            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            PageResponse<PaymentResponse> response = paymentService.findAllWithFilters(pageable, search, status, paymentMethod, startDate, endDate);
            return ApiResponseUtil.buildResponse(
                true,
                "Payments retrieved successfully",
                response,
                "/api/v1/payments"
            );
        } catch (Exception ex) {
            log.error("Error fetching payments: {}", ex.getMessage());
            return ApiResponseUtil.buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to retrieve payments",
                "INTERNAL_SERVER_ERROR",
                "/api/v1/payments"
            );
        }
    }
}