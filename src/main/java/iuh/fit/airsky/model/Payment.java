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

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private LocalDateTime paymentDate;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;  //CREDIT_CARD,PAYPAL,BANK_TRANSFER

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  //  PAID,PENDING,REFUNDED

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;
    
    // Helper method to set the booking and update the back reference
    public void setBooking(Booking booking) {
        if (this.booking == booking) {
            return; // Prevent infinite recursion
        }
        
        Booking oldBooking = this.booking;
        this.booking = booking;
        
        if (oldBooking != null) {
            oldBooking.setPayment(null);
        }
        
        if (booking != null && booking.getPayment() != this) {
            booking.setPayment(this);
        }
    }
}
