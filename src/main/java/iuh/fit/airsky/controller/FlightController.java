package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.FlightRequest;
import iuh.fit.airsky.dto.request.FlightSearchRequest;
import iuh.fit.airsky.dto.response.FlightResponse;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.SeatResponse;
import iuh.fit.airsky.dto.response.RoundTripFlightResponse;
import iuh.fit.airsky.dto.response.UnifiedFlightSearchResponse;
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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
//    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<FlightResponse>> createFlight(@Valid @RequestBody FlightRequest request) {
        try {
            FlightResponse response = flightService.createFlight(request);
            return ApiResponseUtil.buildResponse(true, "Flight created successfully", response, "/api/v1/flights");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Creation failed", ex.getMessage(), "/api/v1/flights");
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = "Validation failed: " + errors.toString();
        return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", errorMessage, "/api/v1/flights");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
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
    // @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER', 'STAFF')")
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
            @RequestParam(value = "startTime", required = false) String startTimeStr,
            @RequestParam(value = "endTime", required = false) String endTimeStr,
            @RequestParam(value = "status", required = false) FlightStatus status,
            Pageable pageable) {
        try {
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            if (startTimeStr != null && !startTimeStr.isEmpty()) {
                if (startTimeStr.length() == 10) { // yyyy-MM-dd
                    startTime = LocalDateTime.parse(startTimeStr + "T00:00:00");
                } else {
                    startTime = LocalDateTime.parse(startTimeStr);
                }
            }
            if (endTimeStr != null && !endTimeStr.isEmpty()) {
                if (endTimeStr.length() == 10) { // yyyy-MM-dd
                    endTime = LocalDateTime.parse(endTimeStr + "T23:59:59");
                } else {
                    endTime = LocalDateTime.parse(endTimeStr);
                }
            }
            PageResponse<FlightResponse> response = flightService.searchFlights(
                    departureAirportId, arrivalAirportId, startTime, endTime, status, null, pageable);
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
    @GetMapping("/{flightId}/seats/{travelClassId}")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsByFlightIdAndTravelClassId(@PathVariable("flightId") Long flightId, @PathVariable("travelClassId") Long travelClassId) {
        List<SeatResponse> seats = seatService.getSeatsByFlightIdAndTravelClassId(flightId,travelClassId);
        return ApiResponseUtil.buildResponse(true, "Seats retrieved successfully", seats, "/api/v1/flights/" + flightId + "/seats/"+travelClassId);
    }

    @GetMapping("/search-oneway")
    public ResponseEntity<ApiResponse<PageResponse<FlightResponse>>> searchOneWayFlights(
            @RequestParam(value = "departureAirportId", required = false) Long departureAirportId,
            @RequestParam(value = "arrivalAirportId", required = false) Long arrivalAirportId,
            @RequestParam(value = "date", required = true) String dateStr,
            @RequestParam(value = "status", required = false) FlightStatus status,
            Pageable pageable) {
        try {
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            if (dateStr != null && !dateStr.isEmpty()) {
                // Chỉ nhận ngày, tự động set giờ từ 00:00:00 đến 23:59:59
                startTime = LocalDateTime.parse(dateStr + "T00:00:00");
                endTime = LocalDateTime.parse(dateStr + "T23:59:59");
            }
            PageResponse<FlightResponse> response = flightService.searchFlights(
                    departureAirportId, arrivalAirportId, startTime, endTime, status, "ONE_WAY", pageable);
            return ApiResponseUtil.buildResponse(true, "One-way flights searched successfully", response, "/api/v1/flights/search-oneway");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed", ex.getMessage(), "/api/v1/flights/search-oneway");
        }
    }

    @GetMapping("/search-roundtrip")
    public ResponseEntity<ApiResponse<RoundTripFlightResponse>> searchRoundTripFlights(
            @RequestParam(value = "departureAirportId") Long departureAirportId,
            @RequestParam(value = "arrivalAirportId") Long arrivalAirportId,
            @RequestParam(value = "outboundDate") String outboundDateStr,
            @RequestParam(value = "returnDate") String returnDateStr,
            @RequestParam(value = "status", required = false) FlightStatus status,
            Pageable pageable) {
        try {
            LocalDateTime outboundDate = null;
            LocalDateTime returnDate = null;
            if (outboundDateStr != null && !outboundDateStr.isEmpty()) {
                outboundDate = LocalDateTime.parse(outboundDateStr + "T00:00:00");
            }
            if (returnDateStr != null && !returnDateStr.isEmpty()) {
                returnDate = LocalDateTime.parse(returnDateStr + "T00:00:00");
            }
            RoundTripFlightResponse response = flightService.searchRoundTripFlights(
                    departureAirportId, arrivalAirportId, outboundDate, returnDate, status, pageable);
            return ApiResponseUtil.buildResponse(true, "Round-trip flights searched successfully", response, "/api/v1/flights/search-roundtrip");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed", ex.getMessage(), "/api/v1/flights/search-roundtrip");
        }
    }

    @GetMapping("/roundtrip-group")
    public ResponseEntity<ApiResponse<PageResponse<FlightResponse>>> findRoundTripFlightsByGroupId(
            @RequestParam("groupId") String groupId,
            Pageable pageable) {
        try {
            PageResponse<FlightResponse> response = flightService.findRoundTripFlightsByGroupId(groupId, pageable);
            return ApiResponseUtil.buildResponse(true, "Round-trip flights by groupId retrieved successfully", response, "/api/v1/flights/roundtrip-group");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed", ex.getMessage(), "/api/v1/flights/roundtrip-group");
        }
    }

    @PostMapping("/search-unified")
    public ResponseEntity<ApiResponse<UnifiedFlightSearchResponse>> searchUnifiedFlights(
            @Valid @RequestBody FlightSearchRequest request,
            Pageable pageable) {
        try {
            UnifiedFlightSearchResponse response = flightService.searchUnifiedFlights(request, pageable);
            return ApiResponseUtil.buildResponse(true, "Flights searched successfully", response, "/api/v1/flights/search-unified");
        } catch (Exception ex) {
            log.error("Error in unified flight search: ", ex);
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed", ex.getMessage(), "/api/v1/flights/search-unified");
        }
    }

    @GetMapping("/check-conflicts")
    public ResponseEntity<ApiResponse<Map<String, List<FlightResponse>>>> checkScheduleConflicts(
            @RequestParam(value = "departureDate", required = false) String departureDate,
            @RequestParam(value = "departureTime", required = false) String departureTime,
            @RequestParam(value = "arrivalDate", required = false) String arrivalDate,
            @RequestParam(value = "arrivalTime", required = false) String arrivalTime,
            @RequestParam(value = "departureAirportId", required = false) Long departureAirportId,
            @RequestParam(value = "arrivalAirportId", required = false) Long arrivalAirportId,
            @RequestParam(value = "aircraftId", required = false) Long aircraftId,
            @RequestParam(value = "gateId", required = false) Long gateId,
            @RequestParam(value = "excludeFlightId", required = false) Long excludeFlightId
    ) {
        try {
            LocalDateTime depTime = null;
            LocalDateTime arrTime = null;
            if (departureDate != null && departureTime != null) {
                depTime = LocalDateTime.parse(departureDate + "T" + departureTime);
            }
            if (arrivalDate != null && arrivalTime != null) {
                arrTime = LocalDateTime.parse(arrivalDate + "T" + arrivalTime);
            }
            Map<String, List<FlightResponse>> conflicts = flightService.checkScheduleConflicts(
                    depTime, arrTime, departureAirportId, arrivalAirportId, aircraftId, gateId, excludeFlightId
            );
            return ApiResponseUtil.buildResponse(true, "Kiểm tra xung đột thành công", conflicts, "/api/v1/flights/check-conflicts");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Kiểm tra xung đột thất bại", ex.getMessage(), "/api/v1/flights/check-conflicts");
        }
    }

    @PostMapping("/compare-prices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> compareFlightPrices(@RequestBody Map<String, Object> params) {
        try {
            Map<String, Object> result = flightService.compareFlightPrices(params);
            return ApiResponseUtil.buildResponse(true, "So sánh giá vé thành công", result, "/api/v1/flights/compare-prices");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "So sánh giá vé thất bại", ex.getMessage(), "/api/v1/flights/compare-prices");
        }
    }
}
