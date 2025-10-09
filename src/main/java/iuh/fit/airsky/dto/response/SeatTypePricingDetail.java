package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.SeatTypes;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SeatTypePricingDetail {
    private String passengerName;
    private String seatNumber;
    private SeatTypes seatType;
    private String seatTypeDescription;
    private BigDecimal additionalPrice;

    public SeatTypePricingDetail(String passengerName, String seatNumber,
                                SeatTypes seatType, BigDecimal additionalPrice) {
        this.passengerName = passengerName;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.seatTypeDescription = seatType.getDisplayName();
        this.additionalPrice = additionalPrice;
    }
}