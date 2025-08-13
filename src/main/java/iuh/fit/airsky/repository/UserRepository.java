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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}