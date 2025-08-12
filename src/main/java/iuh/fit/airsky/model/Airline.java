/*
 * @ (#) Airline.java 1.0 8/12/2025
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
@Table(name = "airlines", indexes = {@Index(name = "idx_airline_code", columnList = "airline_code", unique = true)})
public class Airline extends BaseEntity {

    @Column(nullable = false, unique = true, length = 5)
    private String airlineCode;

    @Column(length = 100)
    private String airlineName;

    @Column(length = 100)
    private String contact;
}