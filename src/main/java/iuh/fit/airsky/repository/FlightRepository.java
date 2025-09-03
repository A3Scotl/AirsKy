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
    // src/main/java/iuh/fit/airsky/repository/FlightRepository.java
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.aircraft.aircraftId = :aircraftId " +
            "AND f.departureTime < :endTime " +
            "AND f.arrivalTime > :startTime")
    boolean existsByAircraftIdAndTimeOverlap(@Param("aircraftId") Long aircraftId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.gate.gateId = :gateId " +
            "AND f.departureTime < :endTime " +
            "AND f.arrivalTime > :startTime")
    boolean existsByGateIdAndTimeOverlap(@Param("gateId") Long gateId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    Optional<Flight> findByFlightNumber(String flightNumber);
    // Tìm chuyến bay nội địa (trong cùng một quốc gia)
    @Query("SELECT f FROM Flight f " +
            "JOIN f.departureAirport da JOIN da.country dc " +
            "JOIN f.arrivalAirport aa JOIN aa.country ac " +
            "WHERE dc.countryName = ac.countryName AND dc.countryName = :country")
    Page<Flight> findDomesticFlights(@Param("country") String country, Pageable pageable);

    // Tìm chuyến bay từ quốc gia A đến quốc gia B
    @Query("SELECT f FROM Flight f " +
            "JOIN f.departureAirport da JOIN da.country dc " +
            "JOIN f.arrivalAirport aa JOIN aa.country ac " +
            "WHERE dc.countryName = :departureCountry AND ac.countryName = :arrivalCountry")
    Page<Flight> findFlightsBetweenCountries(@Param("departureCountry") String departureCountry,
                                             @Param("arrivalCountry") String arrivalCountry,
                                             Pageable pageable);
}