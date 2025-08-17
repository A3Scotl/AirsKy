package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.SeatResponse;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.service.FlightService;
import iuh.fit.airsky.service.SeatService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;
    private final SeatService seatService;

    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlightResponse>> createFlight(@Valid @RequestBody FlightRequest request) {
        try {
            FlightResponse response = flightService.createFlight(request);
            return ApiResponseUtil.buildResponse(true, "Flight created successfully", response, "/api/v1/flights");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/flights");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FlightResponse>> updateFlight(@PathVariable Long id, @Valid @RequestBody FlightRequest request) {
        try {
            FlightResponse response = flightService.updateFlight(id, request);
            return ApiResponseUtil.buildResponse(true, "Flight updated successfully", response, "/api/v1/flights/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/flights/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Update failed", ex.getMessage(), "/api/v1/flights/" + id);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlightResponse>> getFlight(@PathVariable Long id) {
        try {
            return flightService.findById(id)
                    .map(response -> ApiResponseUtil.buildResponse(true, "Flight retrieved successfully", response, "/api/v1/flights/" + id))
                    .orElseGet(() -> ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, "Flight not found", "RESOURCE_NOT_FOUND", "/api/v1/flights/" + id));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/flights/" + id);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FlightResponse>>> getAllFlights(Pageable pageable) {
        try {
            PageResponse<FlightResponse> response = flightService.findAll(pageable);
            return ApiResponseUtil.buildResponse(true, "Flights retrieved successfully", response, "/api/v1/flights");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Retrieval failed", ex.getMessage(), "/api/v1/flights");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<FlightResponse>>> searchFlights(
            @RequestParam Long departureAirportId,
            @RequestParam Long arrivalAirportId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) FlightStatus status,
            Pageable pageable) {
        try {
            PageResponse<FlightResponse> response = flightService.searchFlights(departureAirportId, arrivalAirportId, startTime, endTime, status, pageable);
            return ApiResponseUtil.buildResponse(true, "Flights searched successfully", response, "/api/v1/flights/search");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed", ex.getMessage(), "/api/v1/flights/search");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFlight(@PathVariable Long id) {
        try {
            flightService.delete(id);
            return ApiResponseUtil.buildResponse(true, "Flight deleted successfully", null, "/api/v1/flights/" + id);
        } catch (ResourceNotFoundException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", "/api/v1/flights/" + id);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Deletion failed", ex.getMessage(), "/api/v1/flights/" + id);
        }
    }


    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsByFlight(@PathVariable("id") Long flightId) {
        List<SeatResponse> seats = seatService.getSeatsByFlight(flightId);
        return ApiResponseUtil.buildResponse(true, "Seats retrieved successfully", seats, "/api/v1/flights/" + flightId + "/seats");
    }

}