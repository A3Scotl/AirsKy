package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
public class Airport extends BaseFullSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long airportId;

    @Column(name = "airport_code", length = 5, unique = true)
    private String airportCode;

    @Column(name = "airport_name", length = 100)
    private String airportName;

    @Column(name = "city_name", length = 255)
    private String cityName;

    @Column(name = "thumbnail", length = 500)
    private String thumbnail;

    @Transient
    private List<String> cityNames;

    @PostLoad
    private void loadCityNames() {
        if (cityName != null && !cityName.isEmpty()) {
            cityNames = Arrays.stream(cityName.split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        } else {
            cityNames = new ArrayList<>();
        }
    }

    @PrePersist
    @PreUpdate
    private void saveCityNames() {
        if (cityNames != null && !cityNames.isEmpty()) {
            cityName = String.join(",", cityNames);
        } else if (cityName == null) {
            cityName = "";
        }
        // If cityName is already set (from request mapping), don't overwrite it
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @OneToMany(mappedBy = "airport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Gate> gates;
}