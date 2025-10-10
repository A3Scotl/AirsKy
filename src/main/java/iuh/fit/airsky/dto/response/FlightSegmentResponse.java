package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FlightSegmentResponse {
    private Long segmentId;
    private Integer segmentOrder;
    private Long flightId;
    private String flightNumber;
    private Long classId;
    private String className;
    private AirportResponse departureAirport;
    private AirportResponse arrivalAirport;
    private BigDecimal price;
    private String aircraft;
    private String duration;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
}