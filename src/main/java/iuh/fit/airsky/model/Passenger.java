/*
 * @ (#) Passenger.java 1.0 8/12/2025
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

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "passengers")
public class Passenger extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String passportNumber;

    @Column(length = 10)
    private String type; // Adult, Child, Infant
}