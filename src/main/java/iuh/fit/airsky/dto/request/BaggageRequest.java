package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.BaggageType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BaggageRequest {
    private Long checkinId; // ID của Checkin liên quan
    private BigDecimal weight;
    private BaggageType type;
    private Integer allowance;
}