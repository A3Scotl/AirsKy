package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckinResponse {
    // Basic information
    private Long checkinId;
    private Long bookingId;
    private Long passengerId;
    private String passengerName;

    // Seat information
    private String seatNumber;
    private String seatType;      
    private BigDecimal ticketPrice;

    // Timestamps
    private LocalDateTime issueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Status fields
    private boolean active;
    private LocalDateTime deletedAt;
    private boolean deleted;

    // Boarding pass
    private String boardingPassUrl;

    // Update operation fields (for seat/service changes)
    private String oldSeatNumber;
    private String newSeatNumber;
    private BigDecimal seatChangeCharge;
    private BigDecimal servicesAddedCharge;
    private BigDecimal totalCharge; // Có thể âm nếu refund nhiều hơn charge
    private BigDecimal updatedTotalAmount;
    private String status; // SUCCESS, FAILED, etc.
    private String message;

    // Additional payment fields
    private boolean paymentRequired;
    private Long additionalPaymentId;
    private String additionalPaymentUrl;
}