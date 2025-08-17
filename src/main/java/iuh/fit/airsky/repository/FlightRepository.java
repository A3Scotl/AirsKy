package iuh.fit.airsky.repository;

import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.model.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    @EntityGraph(attributePaths = {"airline", "departureAirport", "arrivalAirport", "gate", "business"})
    Page<Flight> findAll(Pageable pageable);

//    @Query("SELECT f FROM Flight f " +
//            "JOIN FETCH f.airline " +
//            "JOIN FETCH f.departureAirport " +
//            "JOIN FETCH f.arrivalAirport " +
//            "LEFT JOIN FETCH f.gate " +
//            "JOIN FETCH f.business")
//    Page<Flight> findAllWithBusiness(Pageable pageable);


    @Query("SELECT f FROM Flight f WHERE f.departureAirport.airportId = :departureAirportId " +
            "AND f.arrivalAirport.airportId = :arrivalAirportId " +
            "AND f.departureTime BETWEEN :startTime AND :endTime " +
            "AND (:status IS NULL OR f.status = :status)")
    Page<Flight> searchFlights(@Param("departureAirportId") Long departureAirportId,
                               @Param("arrivalAirportId") Long arrivalAirportId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("status") FlightStatus status,
                               Pageable pageable);

    Optional<Flight> findByFlightNumber(String flightNumber);
}