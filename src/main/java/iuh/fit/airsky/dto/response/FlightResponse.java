package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.FlightStatusType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightResponse {
    private Long flightId;
    private String flightNumber;
    private Long airlineId;
    private Long departureAirportId;
    private Long arrivalAirportId;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer duration;
    private String stops;
    private Long gateId;
    private Integer availableSeats;
    private BigDecimal basePrice;
    private FlightStatusType status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}