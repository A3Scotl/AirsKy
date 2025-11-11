package iuh.fit.airsky.model;

import iuh.fit.airsky.enums.BaggageType;
import iuh.fit.airsky.enums.BaggagePackage;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "baggage",
        indexes = {
                @Index(name = "idx_checkin_baggage", columnList = "checkin_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Baggage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long baggageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id")
    private CheckIn checkIn;

    @Enumerated(EnumType.STRING)
    private BaggageType type; // CABIN, CHECK_IN

    // Predefined package selected at booking time
    @Enumerated(EnumType.STRING)
    private BaggagePackage purchasedPackage; // e.g., KG_15, KG_20, ...

    // Fixed price of the selected package at the time of purchase
    @Column(precision = 12, scale = 2)
    private BigDecimal packagePrice;

    // Actual measured weight at airport check-in (nullable until check-in)
    @Column(precision = 5, scale = 2)
    private BigDecimal actualWeight;

    // Excess information determined during check-in (nullable)
    @Column(precision = 5, scale = 2)
    private BigDecimal excessWeight; // max(actualWeight - purchasedPackage.weightKg, 0)

    @Column(precision = 12, scale = 2)
    private BigDecimal excessFee; // fee charged at airport for excess


}
