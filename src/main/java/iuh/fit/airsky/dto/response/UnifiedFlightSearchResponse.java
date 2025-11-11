package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.TripType;
import lombok.Data;

import java.util.List;

@Data
public class UnifiedFlightSearchResponse {
    private TripType tripType;
    private PageResponse<FlightResponse> oneWayFlights; // For ONE_WAY
    private List<RoundTripFlightResponse.RoundTripPair> roundTripPairs; // For ROUND_TRIP
    private List<PageResponse<FlightResponse>> multiCityFlights; // For MULTI_CITY
}
