/*
 * @ (#) AircraftRepository.java 1.0 8/17/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */
package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Aircraft;
import iuh.fit.airsky.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/17/2025
 * @version 1.0
 */
public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByAircraftCode(String code);
    
    @Query("SELECT f FROM Flight f LEFT JOIN FETCH f.aircraft WHERE f.flightId = :flightId")
    Optional<Flight> findFlightWithAircraftById(@Param("flightId") Long flightId);
}
