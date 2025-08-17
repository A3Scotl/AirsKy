package iuh.fit.airsky.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TravelClassResponse {
    private Long classId;
    private String className;
    private String benefits;
    private BigDecimal priceMultiplier;
}