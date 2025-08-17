/*
 * @ (#) Country.java 1.0 8/17/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/17/2025
 * @version 1.0
 */
@Entity
@Table(name="countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Country extends BaseFullSoftDeleteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long countryId;
    @Column(name = "country_code", length = 5, unique = true)
    @NotNull
    @Size(min = 2, max = 5)
    private String countryCode;

    @Column(name = "country_name", length = 100)
    private String countryName;

}