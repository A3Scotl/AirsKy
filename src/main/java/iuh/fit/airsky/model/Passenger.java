package iuh.fit.airsky.model;

import iuh.fit.airsky.enums.PassengerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "passengers",
        indexes = {
                @Index(name = "idx_booking", columnList = "booking_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passengerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String passportNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PassengerType type; //     ADULT,CHILD,INFANT

    @OneToOne
    @JoinColumn(name = "seat_id")
    private Seat seat;
}
