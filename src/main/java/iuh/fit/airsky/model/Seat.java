package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import iuh.fit.airsky.enums.SeatStatus;
import iuh.fit.airsky.enums.SeatTypes;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats",
        indexes = {
                @Index(name = "idx_seat_flight", columnList = "flight_id"),
                @Index(name = "idx_seat_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends BaseFullSoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    @Column(name = "seat_number", length = 5)
    private String seatNumber;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private TravelClass travelClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private SeatStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10)
    private SeatTypes type;

    @ManyToOne
    @JoinColumn(name = "booked_by")
    private Passenger bookedBy;



}

