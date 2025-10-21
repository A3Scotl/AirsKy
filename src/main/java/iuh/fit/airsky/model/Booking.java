package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.util.BookingCodeGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings",
        indexes = {
                @Index(name = "idx_user", columnList = "user_id"),
                @Index(name = "idx_flight", columnList = "flight_id"),
                @Index(name = "idx_class", columnList = "class_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseAuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private TravelClass travelClass;

    private LocalDateTime holdTime; // Thời gian bắt đầu giữ ghế (30 phút để chọn ghế và chuẩn bị thanh toán)
    private LocalDateTime bookingDate;
    private LocalDateTime paymentTimeout; // Thời hạn thanh toán (45 phút tổng thời gian từ lúc tạo booking)

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "booking_code", unique = true, nullable = false, length = 8)
    private String bookingCode;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(name = "contact_email", length = 100)
    private String contactEmail; 

    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        if (this.bookingCode == null) {
            this.bookingCode = BookingCodeGenerator.generateBookingCode();
        }
    }

    // Thêm danh sách hành khách
    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Passenger> passengers = new ArrayList<>();

    // Thêm danh sách check-ins
    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CheckIn> checkIns = new ArrayList<>();

    // Thêm danh sách flight segments
    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FlightSegment> flightSegments = new ArrayList<>();

    // Thêm payment
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;
}
