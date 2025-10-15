package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateBookingTotalRequest {
    private BigDecimal additionalAmount;
    private String reason; // seat_change, services, etc.
    private String description; // Optional detailed description
}