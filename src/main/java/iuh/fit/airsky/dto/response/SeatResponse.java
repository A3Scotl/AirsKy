package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.SeatStatus;
import lombok.Data;

@Data
public class SeatResponse {
    private Long seatId;
    private String seatNumber;
    private String className;
    private SeatStatus status;
    private String bookedBy;
}
