package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.LoyaltyTier;
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
public class DealResponse {
    private Long dealId;
    private String dealCode;
    private String title;
    private BigDecimal discountPercentage;
    private BigDecimal minimumOrderAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String description;
    private String thumbnail;
    private BigDecimal maxDiscountAmount;
    private Integer totalUsageLimit;
    private Integer usedCount;
    private Integer usagePerUser;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Airport info (nếu có)
    private Long departureAirportId;
    private String departureAirportName;
    private String departureAirportCode;
    
    private Long arrivalAirportId;
    private String arrivalAirportName;
    private String arrivalAirportCode;
    
    // Thống kê
    private Integer remainingUsage;
    private String status;

    // Quyền sử dụng deal
    private Boolean isGuestOnly;
    private LoyaltyTier requiredLoyaltyTier;
    private Boolean isLoyaltyExclusive;

    // Points redemption fields
    private Integer pointsRequired;
    private Boolean isPointsRedemption;
    private BigDecimal fixedDiscountAmount;
}
