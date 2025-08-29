package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TravelClassResponse {
    private Long classId;
    private String className;
    private String benefits;
    private BigDecimal priceMultiplier;
    private boolean refundable;
    private boolean changeable;
    private BigDecimal cancellationFee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}