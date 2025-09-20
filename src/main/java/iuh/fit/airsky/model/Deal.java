package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deals",
        indexes = {
                @Index(name = "idx_deal_code", columnList = "deal_code"),
                @Index(name = "idx_deal_valid_from", columnList = "valid_from"),
                @Index(name = "idx_deal_valid_to", columnList = "valid_to"),
                @Index(name = "idx_deal_active", columnList = "is_active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deal extends BaseAuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dealId;

    @Column(name = "deal_code", nullable = false, unique = true, length = 20)
    private String dealCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Column(length = 1000)
    private String description;
    
    private String thumbnail;

    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_airport_id")
    private Airport departureAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_airport_id")
    private Airport arrivalAirport;

    @Builder.Default
    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit = 0;

    @Builder.Default
    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer usagePerUser = 1;

    private Integer earnLoyaltyPoints;  // Tích điểm thưởng
    private Integer redeemLoyaltyPoints;  // Đổi điểm thưởng
}
