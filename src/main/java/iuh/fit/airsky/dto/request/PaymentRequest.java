package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentRequest {
    private Long bookingId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
}