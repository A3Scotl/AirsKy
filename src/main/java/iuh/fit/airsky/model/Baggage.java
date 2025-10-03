package iuh.fit.airsky.model;

import iuh.fit.airsky.enums.BaggageType;
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

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    private BaggageType type; // CABIN,CHECK_IN

    private Integer allowance;
}
