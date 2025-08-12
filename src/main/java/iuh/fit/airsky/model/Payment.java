/*
 * @ (#) Payment.java 1.0 8/12/2025
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
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private LocalDateTime paymentDate;

    @Column(length = 20)
    private String paymentMethod;

    @Column(length = 20)
    private String status;
}