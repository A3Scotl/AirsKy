package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.TripType;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightSearchRequest {
    @NotNull(message = "Trip type is required")
    private TripType tripType;

    // Passenger information (optional)
    @Min(value = 1, message = "At least 1 adult is required")
    private Integer adultCount = 1;

    @Min(value = 0, message = "Child count cannot be negative")
    private Integer childCount = 0;

    @Min(value = 0, message = "Infant count cannot be negative")
    private Integer infantCount = 0;

    // Travel class (optional, defaults to ECONOMY)
    private String travelClass = "ECONOMY"; // ECONOMY, BUSINESS, PREMIUM_ECONOMY, FIRST_CLASS

    // Support for frontend "passengers" field (alternative to individual counts)
    @JsonProperty("passengers")
    private Map<String, Integer> passengers;

    // Unified fields for all trip types (required for ONE_WAY and ROUND_TRIP)
    private Long departureAirportId;

    private Long arrivalAirportId;

    private LocalDate outboundDepartureDate;

    // Only used for ROUND_TRIP
    @FutureOrPresent(message = "Return date must be today or in the future")
    private LocalDate returnDate;

    // Only used for MULTI_CITY
    private List<SearchSegment> multiCityLegs;

    // Derived segments for processing (filled by backend or client based on trip type)
    private List<SearchSegment> segments;

    // Support for frontend "passengers" field mapping
    @JsonProperty("passengers")
    public void setPassengers(Map<String, Integer> passengers) {
        this.passengers = passengers;
        if (passengers != null) {
            if (passengers.containsKey("adults")) {
                this.adultCount = passengers.get("adults");
            }
            if (passengers.containsKey("children")) {
                this.childCount = passengers.get("children");
            }
            if (passengers.containsKey("infants")) {
                this.infantCount = passengers.get("infants");
            }
        }
    }

    @AssertTrue(message = "Outbound departure date must be today or in the future")
    public boolean isValidOutboundDate() {
        if (tripType == TripType.ONE_WAY || tripType == TripType.ROUND_TRIP) {
            return outboundDepartureDate != null && !outboundDepartureDate.isBefore(LocalDate.now());
        }
        return true; // Not required for MULTI_CITY
    }

    @AssertTrue(message = "Required fields are missing or invalid for the selected trip type")
    public boolean isValidFields() {
        if (tripType == null) {
            return false;
        }

        switch (tripType) {
            case ONE_WAY:
                if (departureAirportId == null || arrivalAirportId == null || outboundDepartureDate == null) {
                    return false;
                }
                if (departureAirportId != null && arrivalAirportId != null && departureAirportId.equals(arrivalAirportId)) {
                    return false; // Prevent same airport
                }
                // Construct segments for one-way
                segments = List.of(
                    new SearchSegment(departureAirportId, arrivalAirportId, outboundDepartureDate)
                );
                return true;
            case ROUND_TRIP:
                if (departureAirportId == null || arrivalAirportId == null || outboundDepartureDate == null || returnDate == null) {
                    return false;
                }
                if (departureAirportId != null && arrivalAirportId != null && departureAirportId.equals(arrivalAirportId)) {
                    return false; // Prevent same airport
                }
                if (returnDate != null && outboundDepartureDate != null && !returnDate.isAfter(outboundDepartureDate)) {
                    return false; // Return date must be after outbound
                }
                // Construct segments for round-trip
                segments = List.of(
                    new SearchSegment(departureAirportId, arrivalAirportId, outboundDepartureDate),
                    new SearchSegment(arrivalAirportId, departureAirportId, returnDate)
                );
                return true;
            case MULTI_CITY:
                if (multiCityLegs == null || multiCityLegs.isEmpty()) {
                    return false;
                }
                // Use multiCityLegs as segments
                segments = multiCityLegs;
                return segments.stream().allMatch(seg ->
                    seg.getDepartureAirportId() != null &&
                    seg.getArrivalAirportId() != null &&
                    seg.getDepartureDate() != null &&
                    (seg.getDepartureAirportId() == null || seg.getArrivalAirportId() == null ||
                     !seg.getDepartureAirportId().equals(seg.getArrivalAirportId()))
                );
            default:
                return false;
        }
    }
}