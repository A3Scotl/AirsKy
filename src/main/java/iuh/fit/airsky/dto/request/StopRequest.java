package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StopRequest {
    private Long airportId;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private Integer stopOrder;
}