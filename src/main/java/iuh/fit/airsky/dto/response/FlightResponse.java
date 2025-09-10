package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.FlightType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FlightResponse {
    private Long flightId;
    private String flightNumber;
    private String airlineName;
    private String from;
    private String fromCode;
    private String to;
    private String toCode;
    private String aircraft;
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
    private Integer totalSeats;
    private BigDecimal basePrice;
    private FlightStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}