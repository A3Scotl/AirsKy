package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.FlightType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightResponse {
    private Long flightId;
    private String flightNumber;
    private String airlineName;
    private String departureAirportName;
    private String arrivalAirportName;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;
    private String stops;
    private String gateName;
    private FlightType type;
    private String businessName;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private FlightStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}