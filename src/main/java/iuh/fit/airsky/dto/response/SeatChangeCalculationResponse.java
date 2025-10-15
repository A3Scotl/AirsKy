package iuh.fit.airsky.dto.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SeatChangeCalculationResponse {
    private BigDecimal priceDifference;
    private BigDecimal oldSeatPrice;
    private BigDecimal newSeatPrice;
    private String oldSeatType;
    private String newSeatType;
    private String oldSeatNumber;
    private String newSeatNumber;
    private BigDecimal servicesCharge;
    private List<String> servicesAdded;
    private BigDecimal totalCharge;
    private boolean requiresPayment;
    private String message;
}