/*
 * @ (#) VerificationTokenRepository.java 1.0 8/13/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */
package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.VerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/13/2025
 * @version 1.0
 */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VerificationToken v WHERE v.email = :email")
    Optional<VerificationToken> findByEmailForUpdate(String email);

    void deleteByEmail(String email);
}