package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.BookingStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long bookingId;
    private String userEmail;
    private String flightNumber;
    private String travelClass;
    private LocalDateTime bookingDate;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private List<PassengerSeatResponse> passengers;
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
//    private LocalDateTime holdTime;
}