package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.BaggageType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BaggageResponse {
    private Long baggageId;
    private Long checkinId;
    private BaggageType type;
    private BaggagePackage purchasedPackage;
    private BigDecimal packagePrice;
    private BigDecimal actualWeight;
    private BigDecimal excessWeight;
    private BigDecimal excessFee;
}