package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.FlightType;
import iuh.fit.airsky.enums.TripType;
import iuh.fit.airsky.model.Airline;
import iuh.fit.airsky.model.Airport;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FlightResponse {
    private Long flightId;
    private String flightNumber;
    private AircraftResponse aircraft;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;
    private String stops;
    private List<StopResponse> stopsList;
    private String gate;
    private String terminal;
    private FlightType type;
    private String businessName;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private FlightStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private TripType tripType;
    private String roundTripGroupId; 
    private AirportResponse departureAirport;
    private AirportResponse arrivalAirport;
    private AirlineResponse airline;
    private List<FlightTravelClassResponse> flightTravelClasses;

}