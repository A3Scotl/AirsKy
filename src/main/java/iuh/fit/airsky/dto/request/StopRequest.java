package iuh.fit.airsky.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopRequest {
    private Long airportId;
    private Long flightId;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private String note;
}
