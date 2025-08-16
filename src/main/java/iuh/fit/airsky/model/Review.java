package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
        indexes = {
                @Index(name = "idx_user_review", columnList = "user_id"),
                @Index(name = "idx_flight_review", columnList = "flight_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseAuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // thay vì Long userId


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    private Integer rating;

    @Column(length = 500)
    private String comments;

    private LocalDateTime reviewDate;
}
