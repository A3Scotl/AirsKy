/*
 * @ (#) TravelClass.java 1.0 8/12/2025
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

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "travel_classes")
public class TravelClass extends BaseEntity {

    @Column(length = 20)
    private String className;

    @Column(length = 200)
    private String benefits;

    @Column(precision = 5, scale = 2)
    private BigDecimal priceMultiplier;
}