package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckinResponse {
    private Long checkinId;
    private Long bookingId;
    private Long passengerId;
    private String seatNumber;
    private BigDecimal ticketPrice;
    private LocalDateTime issueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private LocalDateTime deletedAt;
    private boolean deleted;
}