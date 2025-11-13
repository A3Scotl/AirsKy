package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import iuh.fit.airsky.enums.PaymentMethod;
import iuh.fit.airsky.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
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

        @Column(precision = 10, scale = 2)
        private BigDecimal amount;

        private LocalDateTime paymentDate;

        @Enumerated(EnumType.STRING)
        private PaymentMethod paymentMethod;

        @Enumerated(EnumType.STRING)
        @Column(length = 20)
        private PaymentStatus status;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "booking_id", nullable = false)
        private Booking booking;

        @Column(unique = true)
        private String transactionId;

        private String payerId;

        @Column(columnDefinition = "TEXT")
        private String checkoutUrl;

        @Column(name = "payer_account_number")
        private String payerAccountNumber;

        @Column(name = "payer_bank_name")
        private String payerBankName;

        @Column(name = "refund_amount", precision = 10, scale = 2)
        private BigDecimal refundAmount;

        @Column(name = "refund_date")
        private LocalDateTime refundDate;

        @Column(name = "refund_reason", length = 500)
        private String refundReason;

}
