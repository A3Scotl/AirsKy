package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @OneToMany(mappedBy = "airport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Gate> gates;


}
