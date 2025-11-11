package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StopResponse {
    private Long stopId;
    private Long airportId;
    private String airportName;
    private String airportCode;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private Integer stopDuration;
    private Integer stopOrder;
}