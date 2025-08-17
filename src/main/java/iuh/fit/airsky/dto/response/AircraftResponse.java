package iuh.fit.airsky.dto.response;

import lombok.Data;

@Data
public class AircraftResponse {
    private Long aircraftId;
    private String aircraftCode;
    private String aircraftName;
    private Integer totalSeats;
    private String seatLayout;
}
