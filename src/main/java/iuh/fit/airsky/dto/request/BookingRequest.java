package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.ReservationStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingRequest {
    private Long userId;
    private Long flightId;
    private Long classId;
    private LocalDateTime bookingDate;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private Integer adultCount;
    private Integer childCount;
    private Integer infantCount;
}