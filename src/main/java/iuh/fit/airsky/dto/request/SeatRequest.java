package iuh.fit.airsky.dto.request;

import lombok.Data;
import iuh.fit.airsky.enums.SeatStatus;

@Data
public class SeatRequest {
    private Long flightId;
    private Long classId;
    private String seatNumber;
    private SeatStatus status;
    private Long bookedBy;
}
