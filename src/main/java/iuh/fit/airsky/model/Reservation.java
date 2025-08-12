/*
 * @ (#) Reservation.java 1.0 8/12/2025
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
@Table(name = "reservations", indexes = {@Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_flight_id", columnList = "flight_id")})
public class Reservation extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private TravelClass travelClass;

    private LocalDateTime bookingDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 20)
    private String status;

    private Integer adultCount;

    private Integer childCount;

    private Integer infantCount;
}