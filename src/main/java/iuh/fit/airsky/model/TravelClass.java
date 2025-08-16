package iuh.fit.airsky.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "travel_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long classId;

    @Column(name = "class_name", length = 20)
    private String className;

    @Column(length = 200)
    private String benefits;

    @Column(name = "price_multiplier", precision = 5, scale = 2)
    private BigDecimal priceMultiplier;
}
