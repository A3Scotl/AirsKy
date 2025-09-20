package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.PassengerRequest;
import iuh.fit.airsky.dto.response.PassengerResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.PassengerService;
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
@RequestMapping("/api/v1/passengers")
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PassengerResponse>> createPassenger(@Valid @RequestBody PassengerRequest request) {
        try {
            PassengerResponse response = passengerService.createPassenger(request);
            return ApiResponseUtil.buildResponse(true, "Passenger created successfully", response, "/api/v1/passengers");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/passengers");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PassengerResponse>> updatePassenger(@PathVariable Long id, @Valid @RequestBody PassengerRequest request) {
        try {
            PassengerResponse response = passengerService.updatePassenger(id, request);
            return ApiResponseUtil.buildResponse(true, "Passenger updated successfully", response, "/api/v1/passengers/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/passengers/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/passengers/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PassengerResponse>> getPassenger(@PathVariable Long id) {
        try {
            return passengerService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Passenger retrieved successfully", response, "/api/v1/passengers/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Passenger not found", "RESOURCE_NOT_FOUND", "/api/v1/passengers/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/passengers/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PassengerResponse>>> getAllPassengers(Pageable pageable) {
        try {
            PageResponse<PassengerResponse> response = passengerService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Passengers retrieved successfully", response, "/api/v1/passengers");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/passengers");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<Void>> deletePassenger(@PathVariable Long id) {
        try {
            passengerService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Passenger deleted successfully", null, "/api/v1/passengers/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/passengers/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/passengers/" + id);
        }
    }
}