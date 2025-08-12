/*
 * @ (#) Airport.java 1.0 8/12/2025
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

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "airports", indexes = {@Index(name = "idx_airport_code", columnList = "airport_code", unique = true)})
public class Airport extends BaseEntity {

    @Column(nullable = false, unique = true, length = 5)
    private String airportCode;

    @Column(length = 100)
    private String airportName;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String country;
}