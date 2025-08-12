/*
 * @ (#) Role.java 1.0 8/12/2025
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
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String roleName;

    @Column(length = 200)
    private String description;
}
