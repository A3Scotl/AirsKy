package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketRequest {
    private Long bookingId;
    private Long passengerId;
    private String seatNumber;
    private BigDecimal ticketPrice;
    private LocalDateTime issueDate;
}