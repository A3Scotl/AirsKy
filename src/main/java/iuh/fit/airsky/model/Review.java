package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
        indexes = {
                @Index(name = "idx_booking", columnList = "booking_id"),
                @Index(name = "idx_user", columnList = "user_id"),
                @Index(name = "idx_flight", columnList = "flight_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseFullSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column
    private Integer rating;  // 1 to 5

    @Column(length = 1000)
    private String comment;

    private LocalDateTime reviewDate;

    @Builder.Default
    private Boolean isApproved = false;

    @Column
    private LocalDateTime eligibleAt; // Thời điểm đủ điều kiện để review

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    @Column
    private LocalDateTime sentAt; // Thời điểm gửi email

    @Column
    private Integer retryCount;

    @Column(length = 500)
    private String lastError;

    public enum ReviewStatus {
        PENDING,    // Chờ gửi email mời review
        SENT,       // Đã gửi email mời review
        FAILED,     // Gửi email thất bại
        COMPLETED   // Đã review hoặc từ chối review
    }
}