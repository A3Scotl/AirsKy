package iuh.fit.airsky.model;

import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.PassengerType;
import iuh.fit.airsky.enums.Gender;
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

    // Thêm các trường mới (không bắt buộc)
    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    // Chỉ lưu tạm thời trong quá trình booking (không map DB)
    @Transient
    private BaggagePackage tempBaggagePackage;
}

