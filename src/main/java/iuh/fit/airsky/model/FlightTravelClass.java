package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "flight_travel_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightTravelClass extends BaseAuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_class_id", nullable = false)
    private TravelClass travelClass;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price; // Giá cho hạng vé này trong chuyến bay này

    @Column(name = "capacity")
    private Integer capacity; // Tổng số ghế cho hạng vé này
    
    @Column(name = "booked_seat")
    private Integer bookedSeat; // Số ghế đã được đặt
}
