package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TravelClassRequest {
    private String className;
    private String benefits;
    private BigDecimal priceMultiplier;
    private boolean refundable;
    private boolean changeable;
    private BigDecimal cancellationFee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}