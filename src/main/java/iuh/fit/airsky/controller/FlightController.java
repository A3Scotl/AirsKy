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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
//@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;
    private final SeatService seatService;

    public FlightController(FlightService flightService, SeatService seatService) {
        this.flightService = flightService;
        this.seatService = seatService;
    }

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
            @RequestParam(value = "departureAirportId", required = false) Long departureAirportId,
            @RequestParam(value = "arrivalAirportId", required = false) Long arrivalAirportId,
            @RequestParam(value = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(value = "status", required = false) FlightStatus status,
            Pageable pageable) {
        try {
            PageResponse<FlightResponse> response = flightService.searchFlights(
                    departureAirportId, arrivalAirportId, startTime, endTime, status, pageable);
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

    
    @GetMapping("/domestic")
    public ResponseEntity<ApiResponse<PageResponse<FlightResponse>>> findDomesticFlights(
            @RequestParam String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            log.info("Finding domestic flights in {} - page: {}, size: {}", country, page, size);

            // Validate input parameter
            if (country == null || country.trim().isEmpty()) {
                return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Country is required", "COUNTRY_REQUIRED", "/api/v1/flights/domestic");
            }

            Pageable pageable = PageRequest.of(page, size);
            PageResponse<FlightResponse> response = flightService.findDomesticFlights(country.trim(), pageable);

            return ApiResponseUtil.buildResponse(true,
                    String.format("Domestic flights in %s retrieved successfully", country),
                    response, "/api/v1/flights/domestic");

        } catch (Exception ex) {
            log.error("Error finding domestic flights: ", ex);
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve domestic flights", ex.getMessage(), "/api/v1/flights/domestic");
        }
    }

    @GetMapping("/between-countries")
    public ResponseEntity<ApiResponse<PageResponse<FlightResponse>>> findFlightsBetweenCountries(
            @RequestParam String departureCountry,
            @RequestParam String arrivalCountry,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            log.info("Finding flights from {} to {} - page: {}, size: {}", departureCountry, arrivalCountry, page, size);

            // Validate input parameters
            if (departureCountry == null || departureCountry.trim().isEmpty()) {
                return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Departure country is required", "DEPARTURE_COUNTRY_REQUIRED", "/api/v1/flights/between-countries");
            }

            if (arrivalCountry == null || arrivalCountry.trim().isEmpty()) {
                return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST,
                        "Arrival country is required", "ARRIVAL_COUNTRY_REQUIRED", "/api/v1/flights/between-countries");
            }

            Pageable pageable = PageRequest.of(page, size);
            PageResponse<FlightResponse> response = flightService.findFlightsBetweenCountries(
                    departureCountry.trim(), arrivalCountry.trim(), pageable);

            return ApiResponseUtil.buildResponse(true,
                    String.format("Flights from %s to %s retrieved successfully", departureCountry, arrivalCountry),
                    response, "/api/v1/flights/between-countries");

        } catch (Exception ex) {
            log.error("Error finding flights between countries: ", ex);
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to retrieve flights between countries", ex.getMessage(), "/api/v1/flights/between-countries");
        }
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsByFlight(@PathVariable("id") Long flightId) {
        List<SeatResponse> seats = seatService.getSeatsByFlight(flightId);
        return ApiResponseUtil.buildResponse(true, "Seats retrieved successfully", seats, "/api/v1/flights/" + flightId + "/seats");
    }
    @GetMapping("/{id}/seats/{travelClassId}")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsByFlightAndTravelClass(@PathVariable("id") Long flightId,@PathVariable("travelClassId") Long travelClassId) {
        List<SeatResponse> seats = seatService.getSeatsByFlightAndTravelClass(flightId,travelClassId);
        return ApiResponseUtil.buildResponse(true, "Seats retrieved successfully", seats, "/api/v1/flights/" + flightId + "/seats/" + travelClassId);
    }

}