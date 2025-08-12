/*
 * @ (#) Flight.java 1.0 8/12/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.model;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/12/2025
 * @version 1.0
 */
import iuh.fit.airsky.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "flights", indexes = {@Index(name = "idx_flight_number", columnList = "flight_number"),
        @Index(name = "idx_departure_time", columnList = "departure_time")})
public class Flight extends BaseEntity {

    @Column(length = 10)
    private String flightNumber;

    @ManyToOne
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne
    @JoinColumn(name = "departure_airport_id", nullable = false)
    private Airport departureAirport;

    @ManyToOne
    @JoinColumn(name = "arrival_airport_id", nullable = false)
    private Airport arrivalAirport;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    private Integer duration; // minutes

    @Column(length = 20)
    private String stops;

    private Integer availableSeats;

    @Column(precision = 10, scale = 2)
    private BigDecimal basePrice;
}