/*
 * @ (#) BaseAuditOnlyEntity.java 1.0 8/16/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/16/2025
 * @version 1.0
 */
@Data
@MappedSuperclass
public class BaseAuditOnlyEntity {
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
