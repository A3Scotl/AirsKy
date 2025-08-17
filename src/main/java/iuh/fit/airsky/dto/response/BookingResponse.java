package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.ReservationStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long bookingId;
    private Long userId;
    private Long flightId;
    private Long classId;
    private LocalDateTime bookingDate;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private List<PassengerSeatResponse> passengers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}