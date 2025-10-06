package iuh.fit.airsky.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingAncillaryServiceResponse {
    
    private Long bookingServiceId;
    private Long serviceId;
    private String serviceName;
    private String serviceType;
    private String serviceTypeDisplayName;
    private Long passengerId;
    private String passengerName; // firstName + lastName
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;
}