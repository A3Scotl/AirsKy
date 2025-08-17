package iuh.fit.airsky.dto.request;

import lombok.Data;

@Data
public class AircraftRequest {
    private String aircraftCode;
    private String aircraftName;
    private Integer totalSeats;
    private String seatLayout; // ví dụ "3-3" hoặc "4-3"
}
