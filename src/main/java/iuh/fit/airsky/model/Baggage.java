package iuh.fit.airsky.model;

import iuh.fit.airsky.enums.BaggageType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "baggage",
        indexes = {
                @Index(name = "idx_ticket_baggage", columnList = "ticket_id")
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
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;
    @Enumerated(EnumType.STRING)
    private BaggageType type; // CABIN,CHECK_IN

    private Integer allowance;
}
