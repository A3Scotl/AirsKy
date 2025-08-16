package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.AirportRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.AirportService;
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
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirportResponse>> createAirport(@Valid @RequestBody AirportRequest request) {
        try {
            AirportResponse response = airportService.createAirport(request);
            return ApiResponseUtil.buildResponse(true, "Airport created successfully", response, "/api/v1/airports");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/airports");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AirportResponse>> updateAirport(@PathVariable Long id, @Valid @RequestBody AirportRequest request) {
        try {
            AirportResponse response = airportService.updateAirport(id, request);
            return ApiResponseUtil.buildResponse(true, "Airport updated successfully", response, "/api/v1/airports/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/airports/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/airports/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AirportResponse>> getAirport(@PathVariable Long id) {
        try {
            return airportService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Airport retrieved successfully", response, "/api/v1/airports/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Airport not found", "RESOURCE_NOT_FOUND", "/api/v1/airports/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airports/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AirportResponse>>> getAllAirports(Pageable pageable) {
        try {
            PageResponse<AirportResponse> response = airportService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Airports retrieved successfully", response, "/api/v1/airports");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/airports");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAirport(@PathVariable Long id) {
        try {
            airportService.softDelete(id);
            return ApiResponseUtil.buildResponse(true, "Airport soft deleted successfully", null, "/api/v1/airports/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/airports/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/airports/" + id);
        }
    }
}