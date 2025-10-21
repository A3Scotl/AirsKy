package iuh.fit.airsky.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.FlightType;
import iuh.fit.airsky.enums.TripType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "flights",
        indexes = {
                @Index(name = "idx_flight_number", columnList = "flight_number"),
                @Index(name = "idx_airline", columnList = "airline_id"),
                @Index(name = "idx_departure_airport", columnList = "departure_airport_id"),
                @Index(name = "idx_arrival_airport", columnList = "arrival_airport_id"),
                @Index(name = "idx_flight_status", columnList = "status"),
                @Index(name = "idx_departure_arrival", columnList = "departure_airport_id,arrival_airport_id")

        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight  extends BaseAuditOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long flightId;

    @Column(name = "flight_number", length = 10)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_airport_id", nullable = false)
    private Airport departureAirport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_airport_id", nullable = false)
    private Airport arrivalAirport;

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private Integer duration;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "business_id", nullable = false)
    private User business;

    @Column(length = 20)
    private String stops;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Stop> stopsList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate_id")
    private Gate gate;

    private Integer availableSeats;

    @Column(precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private FlightStatus status;//    ON_TIME,DELAYED,CANCELLED

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private FlightType type;//    DOMESTIC , INTERNATIONAL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TripType tripType; // ONE_WAY, ROUND_TRIP, MULTI_CITY

    @Column(name = "round_trip_group_id")
    private String roundTripGroupId; // dùng để liên kết các chuyến bay khứ hồi

    @Version
    @Builder.Default
    private Integer version = 0;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<FlightTravelClass> flightTravelClasses = new ArrayList<>();

    @PostLoad
    private void loadStopsString() {
        if (stopsList == null || stopsList.isEmpty()) {
            stops = "NON_STOP";
        } else {
            stops = stopsList.stream()
                    .filter(stop -> stop.getAirport() != null)
                    .map(stop -> stop.getAirport().getAirportCode())
                    .collect(Collectors.joining(","));
        }
    }
}