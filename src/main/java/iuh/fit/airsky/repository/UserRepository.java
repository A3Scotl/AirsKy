/*
 * @ (#) UserRepository.java 1.0 8/13/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */
package iuh.fit.airsky.repository;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/13/2025
 * @version 1.0
 */

import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.deleted = false")
    Page<User> findAll(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deleted = false")
    Optional<User> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.deleted = true, u.deletedAt = :now, u.active = false WHERE u.id = :id")
    void softDeleteById(Long id, LocalDateTime now);
}