package iuh.fit.airsky.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealUsageResponse {
    private Long usageId;
    private BigDecimal discountAmount;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    private LocalDateTime createdAt;
    
    // Deal info
    private Long dealId;
    private String dealCode;
    private String dealTitle;
    
    // User info
    private Long userId;
    private String userName;
    private String userEmail;
    
    // Booking info
    private Long bookingId;
    private String bookingCode;
}
