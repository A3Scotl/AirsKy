package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.TravelClassRequest;
import iuh.fit.airsky.dto.response.AirportResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.TravelClassResponse;
import iuh.fit.airsky.service.TravelClassService;
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
@RequestMapping("/api/v1/travel-classes")
@RequiredArgsConstructor
public class TravelClassController {

    private final TravelClassService travelClassService;
    private static final String BASE_PATH = "/api/v1/travel-classes";

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TravelClassResponse>> createTravelClass(
            @Valid @RequestBody TravelClassRequest request) {
        try {
            log.info("Creating new travel class: {}", request.getClassName());
            TravelClassResponse response = travelClassService.createTravelClass(request);
            String resourcePath = String.format("%s/%d", BASE_PATH, response.getClassId());

            return ApiResponseUtil.buildResponse(
                    true,
                    "Travel class created successfully",
                    response,
                    resourcePath
            );
        } catch (Exception e) {
            log.error("Error creating travel class: {}", e.getMessage(), e);
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Failed to create travel class: " + e.getMessage(),
                    "TRAVEL_CLASS_CREATION_FAILED",
                    BASE_PATH
            );
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TravelClassResponse>> updateTravelClass(
            @PathVariable Long id,
            @Valid @RequestBody TravelClassRequest request) {
        try {
            log.info("Updating travel class with ID: {}", id);
            TravelClassResponse response = travelClassService.updateTravelClass(id, request);
            String resourcePath = String.format("%s/%d", BASE_PATH, id);

            return ApiResponseUtil.buildResponse(
                    true,
                    "Travel class updated successfully",
                    response,
                    resourcePath
            );
        } catch (Exception e) {
            log.error("Error updating travel class with ID {}: {}", id, e.getMessage(), e);
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Failed to update travel class: " + e.getMessage(),
                    "TRAVEL_CLASS_UPDATE_FAILED",
                    String.format("%s/%d", BASE_PATH, id)
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TravelClassResponse>> getTravelClassById(@PathVariable Long id) {
        try {
            log.info("Fetching travel class with ID: {}", id);
            String resourcePath = String.format("%s/%d", BASE_PATH, id);

            return travelClassService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(
                            true,
                            "Travel class retrieved successfully",
                            response,
                            resourcePath
                    ))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(
                            HttpStatus.NOT_FOUND,
                            "Travel class not found with id: " + id,
                            "TRAVEL_CLASS_NOT_FOUND",
                            resourcePath
                    ));
        } catch (Exception e) {
            log.error("Error fetching travel class with ID {}: {}", id, e.getMessage(), e);
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while fetching travel class",
                    "INTERNAL_SERVER_ERROR",
                    String.format("%s/%d", BASE_PATH, id)
            );
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TravelClassResponse>>> getAllTravelClasses(Pageable pageable) {
        try {
            log.info("Fetching all travel classes with pagination: {}", pageable);
            return ApiResponseUtil.buildResponse(
                    true,
                    "Travel classes retrieved successfully",
                    travelClassService.findAll(pageable),
                    BASE_PATH
            );
        } catch (Exception e) {
            log.error("Error fetching travel classes: {}", e.getMessage(), e);
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve travel classes: " + e.getMessage(),
                    "TRAVEL_CLASSES_RETRIEVAL_FAILED",
                    BASE_PATH
            );
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTravelClass(@PathVariable Long id) {
        try {
            log.info("Deleting travel class with ID: {}", id);
            travelClassService.delete(id);

            return ApiResponseUtil.buildResponse(
                    true,
                    "Travel class deleted successfully",
                    null,
                    String.format("%s/%d", BASE_PATH, id)
            );
        } catch (Exception e) {
            log.error("Error deleting travel class with ID {}: {}", id, e.getMessage(), e);
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Failed to delete travel class: " + e.getMessage(),
                    "TRAVEL_CLASS_DELETION_FAILED",
                    String.format("%s/%d", BASE_PATH, id)
            );
        }
    }
}