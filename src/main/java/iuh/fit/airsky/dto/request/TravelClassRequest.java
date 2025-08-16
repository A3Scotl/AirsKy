package iuh.fit.airsky.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TravelClassRequest {
    private String className;
    private String benefits;
    private BigDecimal priceMultiplier;
}