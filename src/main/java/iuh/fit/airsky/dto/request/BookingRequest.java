package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.CheckInType;
import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.BookingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class BookingRequest {
    private Long userId;
    private Long flightId;
    private Long classId;
    private LocalDateTime bookingDate;
    private LocalDateTime holdTime;
    private BigDecimal totalAmount;
    private BookingStatus status = BookingStatus.PENDING;
    private List<PassengerSeatRequest> passengers;
    private PaymentMethod paymentMethod;
    private CheckInType checkInType = CheckInType.OFFLINE;

}