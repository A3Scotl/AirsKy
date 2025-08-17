package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_booking_payment", columnList = "booking_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseAuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private LocalDateTime paymentDate;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;  //CREDIT_CARD,PAYPAL,BANK_TRANSFER

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  //  PAID,PENDING,REFUNDED
}
