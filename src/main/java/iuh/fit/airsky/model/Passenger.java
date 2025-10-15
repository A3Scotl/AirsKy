package iuh.fit.airsky.model;

import iuh.fit.airsky.enums.BaggagePackage;
import iuh.fit.airsky.enums.PassengerType;
import iuh.fit.airsky.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    // Thay đổi: Xóa @OneToOne với Seat, thêm List<PassengerSeatAssignment>
    @Builder.Default
    @OneToMany(mappedBy = "passenger", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PassengerSeatAssignment> seatAssignments = new ArrayList<>();

    // Thêm các trường mới (không bắt buộc)
    @Column(length = 100)
    private String email;

    @Column(length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    // Helper methods để tương thích ngược
    public Seat getPrimarySeat() {
        return seatAssignments.stream()
                .filter(assignment -> assignment.getFlightSegment().getSegmentOrder() == 1)
                .findFirst()
                .map(PassengerSeatAssignment::getSeat)
                .orElse(null);
    }

    // Method tương thích ngược - trả về primary seat
    public Seat getSeat() {
        return getPrimarySeat();
    }

    // Method tương thích ngược - set primary seat (chỉ dùng cho backward compatibility)
    public void setSeat(Seat seat) {
        // Đồng bộ với assignment nếu có
        if (seat != null) {
            seatAssignments.stream()
                    .filter(assignment -> assignment.getFlightSegment().getSegmentOrder() == 1)
                    .findFirst()
                    .ifPresent(assignment -> assignment.setSeat(seat));
        }
    }

    public List<Seat> getAllSeats() {
        return seatAssignments.stream()
                .map(PassengerSeatAssignment::getSeat)
                .toList();
    }

    // Chỉ lưu tạm thời trong quá trình booking (không map DB)
    @Transient
    private BaggagePackage tempBaggagePackage;
}

