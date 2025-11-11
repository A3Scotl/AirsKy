package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import iuh.fit.airsky.enums.CheckinStatus;
import iuh.fit.airsky.enums.CheckInType;
import iuh.fit.airsky.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "checkins",
        indexes = {
                @Index(name = "idx_booking_checkin", columnList = "booking_id"),
                @Index(name = "idx_passenger_checkin", columnList = "passenger_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckIn extends BaseFullSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkInId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = true)
    private FlightSegment flightSegment;

    @Column(length = 10)
    private String seatNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    private LocalDateTime checkedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_type", length = 10)
    private CheckInType checkInType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CheckinStatus status;

    private String boardingPassUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baggage_id", nullable = true)
    private Baggage baggage;

}
