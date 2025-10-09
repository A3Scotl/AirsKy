package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckinRequest {
    private Long bookingId;
    private Long passengerId;
    private String seatNumber;
    private BigDecimal ticketPrice;
}