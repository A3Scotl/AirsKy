/*
 * @ (#) CountryRepository.java 1.0 8/17/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */
package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/17/2025
 * @version 1.0
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    @Query("SELECT c FROM Country c WHERE c.deleted = false")
    Page<Country> findAll(Pageable pageable);

    @Query("SELECT c FROM Country c WHERE c.countryId = :id AND c.deleted = false")
    Optional<Country> findById(Long id);


    Optional<Country> findByCountryCode(String countryCode);

    @Query("SELECT c FROM Country c WHERE LOWER(c.countryName) LIKE LOWER(CONCAT('%', :countryName, '%')) AND c.deleted = false")
    Page<Country> findByCountryNameContaining(String countryName, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Country c SET c.deleted = true, c.deletedAt = :now, c.active = false WHERE c.countryId = :id")
    void softDeleteById(Long id, LocalDateTime now);
}
