package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "airports",
        indexes = {
                @Index(name = "idx_airport_code", columnList = "airport_code")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airport  extends BaseFullSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long airportId;

    @Column(name = "airport_code", length = 5, unique = true)
    private String airportCode;

    @Column(name = "airport_name", length = 100)
    private String airportName;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String country;

}
