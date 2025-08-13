/*
 * @ (#) VerificationToken.java 1.0 8/13/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */
package iuh.fit.airsky.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "resend_count")
    private int resendCount;

    @Column(name = "last_resend_time")
    private LocalDateTime lastResendTime;
}