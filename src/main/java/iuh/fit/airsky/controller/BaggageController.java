package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.BaggageRequest;
import iuh.fit.airsky.dto.response.BaggageResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.BaggageService;
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
@RequestMapping("/api/v1/baggages")
@RequiredArgsConstructor
public class BaggageController {

    private final BaggageService baggageService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BaggageResponse>> createBaggage(@Valid @RequestBody BaggageRequest request) {
        try {
            BaggageResponse response = baggageService.createBaggage(request);
            return ApiResponseUtil.buildResponse(true, "Baggage created successfully", response, "/api/v1/baggages");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/baggages");
        }
    }

//    @PutMapping("/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<BaggageResponse>> updateBaggage(@PathVariable Long id, @Valid @RequestBody BaggageRequest request) {
//        try {
//            BaggageResponse response = baggageService.updateBaggage(id, request);
//            return ApiResponseUtil.buildResponse(true, "Baggage updated successfully", response, "/api/v1/baggages/" + id);
//        } catch (ResourceNotFoundException ex) {
//            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/baggages/" + id);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/baggages/" + id);
//        }
//    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BaggageResponse>> getBaggage(@PathVariable Long id) {
        try {
            return baggageService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Baggage retrieved successfully", response, "/api/v1/baggages/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Baggage not found", "RESOURCE_NOT_FOUND", "/api/v1/baggages/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/baggages/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BaggageResponse>>> getAllBaggages(Pageable pageable) {
        try {
            PageResponse<BaggageResponse> response = baggageService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Baggages retrieved successfully", response, "/api/v1/baggages");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/baggages");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBaggage(@PathVariable Long id) {
        try {
            baggageService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Baggage deleted successfully", null, "/api/v1/baggages/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/baggages/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/baggages/" + id);
        }
    }
}