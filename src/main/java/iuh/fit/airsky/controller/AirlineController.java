package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.AirlineRequest;
import iuh.fit.airsky.dto.response.AirlineResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.AirlineService;
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
@RequestMapping("/api/v1/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirlineResponse>> createAirline(@Valid @RequestBody AirlineRequest request) {
        try {
            AirlineResponse response = airlineService.createAirline(request);
            return ApiResponseUtil.buildResponse(true, "Airline created successfully", response, "/api/v1/airlines");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/airlines");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirlineResponse>> updateAirline(@PathVariable Long id, @Valid @RequestBody AirlineRequest request) {
        try {
            AirlineResponse response = airlineService.updateAirline(id, request);
            return ApiResponseUtil.buildResponse(true, "Airline updated successfully", response, "/api/v1/airlines/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/airlines/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/airlines/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AirlineResponse>> getAirline(@PathVariable Long id) {
        try {
            return airlineService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Airline retrieved successfully", response, "/api/v1/airlines/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Airline not found", "RESOURCE_NOT_FOUND", "/api/v1/airlines/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airlines/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AirlineResponse>>> getAllAirlines(Pageable pageable) {
        try {
            PageResponse<AirlineResponse> response = airlineService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Airlines retrieved successfully", response, "/api/v1/airlines");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airlines");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAirline(@PathVariable Long id) {
        try {
            airlineService.softDelete(id);
            return ApiResponseUtil.buildResponse(true, "Airline soft deleted successfully", null, "/api/v1/airlines/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/airlines/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/airlines/" + id);
        }
    }
}