/*
 * @ (#) BaseEntity.java 1.1 8/12/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.base;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * BasefullsoftdeleteEntity - abstract class cho tất cả entity
 * Quản lý common fields: active ,createdAt, updatedAt, deletedAt (soft delete)
 */

@Data
@MappedSuperclass
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseFullSoftDeleteEntity {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Builder.Default
    @Column(name = "is_active")
    private boolean active=true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * Đánh dấu xóa mềm
     */
    public void softDelete() {
        this.deleted = true;
        this.active = false;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Khôi phục lại (nếu cần)
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }
}
