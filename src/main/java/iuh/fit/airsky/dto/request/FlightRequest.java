package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.FlightType;
import iuh.fit.airsky.model.Aircraft;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightRequest {
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
    private FlightStatus status;
    private FlightType type;
    private Long businessId;
    private Aircraft aircraft;


}