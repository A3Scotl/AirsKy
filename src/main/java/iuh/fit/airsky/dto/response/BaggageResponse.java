package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.BaggageType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BaggageResponse {
    private Long baggageId;
    private Long checkinId;
    private BigDecimal weight;
    private BaggageType type;
    private Integer allowance;
}