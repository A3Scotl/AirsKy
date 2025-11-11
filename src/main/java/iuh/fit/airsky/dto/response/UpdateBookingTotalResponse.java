package iuh.fit.airsky.dto.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
public class UpdateBookingTotalResponse {
    private Long bookingId;
    private String bookingCode;
    private BigDecimal oldTotalAmount;
    private BigDecimal additionalAmount;
    private BigDecimal newTotalAmount;
    private String reason;
    private String message;
}