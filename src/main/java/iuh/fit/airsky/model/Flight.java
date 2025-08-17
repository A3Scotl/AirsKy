package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseAuditOnlyEntity;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.FlightType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

}
