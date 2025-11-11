package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.SeatStatus;
import lombok.Data;

@Data
public class PassengerSeatAssignmentResponse {
    private Long assignmentId;
    private Integer segmentOrder;
    private String seatNumber;
    private String seatType;
    private SeatStatus status;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
}