package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stops",
       indexes = {
           @Index(name = "idx_flight_stop", columnList = "flight_id"),
           @Index(name = "idx_airport_stop", columnList = "airport_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stop extends BaseAuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight; // chuyến bay cha

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id", nullable = false)
    private Airport airport; // sân bay dừng

    private LocalDateTime arrivalTime;   // thời gian đến điểm dừng
    private LocalDateTime departureTime; // thời gian rời điểm dừng

    @Column(length = 255)
    private String note; // ghi chú (nếu cần, ví dụ "tiếp nhiên liệu", "quá cảnh")
}
