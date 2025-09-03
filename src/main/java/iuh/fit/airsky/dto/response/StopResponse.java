package iuh.fit.airsky.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopResponse {
    private Long stopId;
    private Long flightId;
    private Long airportId;
    private String airportName;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private String note;
}
