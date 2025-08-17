/*
 * @ (#) Gate.java 1.0 8/16/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/16/2025
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "gates")
public class Gate extends BaseFullSoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id", nullable = false)
    private Airport airport;

    @Column(length = 10)
    private String gateName;

    @OneToMany(mappedBy = "gate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Flight> flights;
}
