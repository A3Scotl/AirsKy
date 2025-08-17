package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.PaymentRequest;
import iuh.fit.airsky.dto.response.PaymentResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.createPayment(request);
            return ApiResponseUtil.buildResponse(true, "Payment created successfully", response, "/api/v1/payments");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/payments");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.updatePayment(id, request);
            return ApiResponseUtil.buildResponse(true, "Payment updated successfully", response, "/api/v1/payments/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/payments/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/payments/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long id) {
        try {
            return paymentService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Payment retrieved successfully", response, "/api/v1/payments/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Payment not found", "RESOURCE_NOT_FOUND", "/api/v1/payments/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/payments/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getAllPayments(Pageable pageable) {
        try {
            PageResponse<PaymentResponse> response = paymentService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Payments retrieved successfully", response, "/api/v1/payments");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/payments");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id) {
        try {
            paymentService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Payment deleted successfully", null, "/api/v1/payments/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/payments/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/payments/" + id);
        }
    }
}