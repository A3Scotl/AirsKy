package iuh.fit.airsky.repository;

import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.TripType;
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
    @EntityGraph(attributePaths = {"airline", "departureAirport", "arrivalAirport", "gate", "business","stops"})
    Page<Flight> findAll(Pageable pageable);

    @Query("SELECT f FROM Flight f WHERE " +
            "(:departureAirportId IS NULL OR f.departureAirport.airportId = :departureAirportId) " +
            "AND (:arrivalAirportId IS NULL OR f.arrivalAirport.airportId = :arrivalAirportId) " +
            "AND f.departureTime BETWEEN :startTime AND :endTime " +
            "AND (:status IS NULL OR f.status = :status) " +
            "AND (:tripType IS NULL OR f.tripType = :tripType)")
    Page<Flight> searchFlights(@Param("departureAirportId") Long departureAirportId,
                               @Param("arrivalAirportId") Long arrivalAirportId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("status") FlightStatus status,
                               @Param("tripType") TripType tripType,
                               Pageable pageable);

    // Kiểm tra overlap tại sân bay khởi hành
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

    @Query("SELECT f FROM Flight f WHERE f.roundTripGroupId = :groupId AND f.tripType = 'ROUND_TRIP'")
    Page<Flight> findRoundTripFlightsByGroupId(@Param("groupId") String groupId, Pageable pageable);

    @Query("SELECT f FROM Flight f WHERE f.roundTripGroupId IS NOT NULL AND f.tripType = 'ROUND_TRIP'")
    Page<Flight> findAllRoundTripFlights(Pageable pageable);

    // Kiểm tra overlap tại sân bay khởi hành
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.departureAirport.airportId = :airportId " +
            "AND ((f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime) OR " +
            "(f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime)) " +
            "AND (:excludeId IS NULL OR f.flightId != :excludeId)")
    boolean existsByDepartureAirportIdAndTimeOverlap(@Param("airportId") Long airportId,
                                                     @Param("departureTime") LocalDateTime departureTime,
                                                     @Param("arrivalTime") LocalDateTime arrivalTime,
                                                     @Param("excludeId") Long excludeId);

    // Kiểm tra overlap tại sân bay hạ cánh
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.arrivalAirport.airportId = :airportId " +
            "AND ((f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime) OR " +
            "(f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime)) " +
            "AND (:excludeId IS NULL OR f.flightId != :excludeId)")
    boolean existsByArrivalAirportIdAndTimeOverlap(@Param("airportId") Long airportId,
                                                   @Param("departureTime") LocalDateTime departureTime,
                                                   @Param("arrivalTime") LocalDateTime arrivalTime,
                                                   @Param("excludeId") Long excludeId);

    // Đếm số chuyến bay tại sân bay trong khoảng thời gian (cho buffer time)
    @Query("SELECT COUNT(f) FROM Flight f WHERE " +
            "(f.departureAirport.airportId = :airportId OR f.arrivalAirport.airportId = :airportId) " +
            "AND ((f.departureTime BETWEEN :start AND :end) OR (f.arrivalTime BETWEEN :start AND :end))")
    long countFlightsAtAirportInTimeRange(@Param("airportId") Long airportId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}