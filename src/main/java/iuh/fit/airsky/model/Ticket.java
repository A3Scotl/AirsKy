package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets",
        indexes = {
                @Index(name = "idx_booking_ticket", columnList = "booking_id"),
                @Index(name = "idx_passenger_ticket", columnList = "passenger_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket extends BaseFullSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(length = 10)
    private String seatNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    private LocalDateTime issueDate;
}
