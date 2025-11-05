/*
 * @ (#) Aircraft.java 1.0 8/17/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/17/2025
 * @version 1.0
 */
@Entity
@Table(name="aircrafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aircraft extends BaseFullSoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aircraftId;
    @Column(name = "aircraft_code", length = 5, unique = true)
    @NotNull
    @Size(min = 2, max = 5)
    private String aircraftCode;

    @Column(name = "aircraft_name", length = 100)
    private String aircraftName;


    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "seat_layout", length = 10)
    @NotNull
    @Pattern(regexp = "^\\d+(-\\d+)+$", message = "Seat layout must be in format like '3-3', '3-2-3', '2-4-2'")
    private String seatLayout; // ví dụ "3-3" hoặc "3-2-3"
}
