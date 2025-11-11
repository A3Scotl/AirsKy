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
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    @EntityGraph(attributePaths = {"airline", "departureAirport", "arrivalAirport", "gate", "business","stops"})
    Page<Flight> findAll(Pageable pageable);

    @Query("SELECT f FROM Flight f WHERE " +
            "(:departureAirportId IS NULL OR f.departureAirport.airportId = :departureAirportId) " +
            "AND (:arrivalAirportId IS NULL OR f.arrivalAirport.airportId = :arrivalAirportId) " +
            "AND f.departureTime BETWEEN :startTime AND :endTime " +
            "AND ((:statuses) IS NULL OR f.status IN (:statuses)) " +
            "AND (:tripType IS NULL OR f.tripType = :tripType)")
    Page<Flight> searchFlights(@Param("departureAirportId") Long departureAirportId,
                               @Param("arrivalAirportId") Long arrivalAirportId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime,
                               @Param("statuses") List<FlightStatus> statuses,
                               @Param("tripType") TripType tripType,
                               Pageable pageable);

    // Kiểm tra overlap tại sân bay khởi hành
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.aircraft.aircraftId = :aircraftId " +
            "AND f.departureTime < :endTime " +
            "AND f.arrivalTime > :startTime " +
            "AND f.status <> iuh.fit.airsky.enums.FlightStatus.DEPARTED " +
            "AND f.status <> iuh.fit.airsky.enums.FlightStatus.CANCELLED")
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

    // Trả về danh sách chuyến bay xung đột theo máy bay
    @Query("SELECT f FROM Flight f WHERE f.aircraft.aircraftId = :aircraftId AND ((f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime)) AND (:excludeId = -1 OR f.flightId != :excludeId)")
    List<Flight> findAircraftConflicts(@Param("aircraftId") Long aircraftId,
                                       @Param("departureTime") LocalDateTime departureTime,
                                       @Param("arrivalTime") LocalDateTime arrivalTime,
                                       @Param("excludeId") Long excludeId);

    // Trả về danh sách chuyến bay xung đột theo cổng
    @Query("SELECT f FROM Flight f WHERE f.gate.gateId = :gateId AND ((f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime)) AND (:excludeId = -1 OR f.flightId != :excludeId)")
    List<Flight> findGateConflicts(@Param("gateId") Long gateId,
                                   @Param("departureTime") LocalDateTime departureTime,
                                   @Param("arrivalTime") LocalDateTime arrivalTime,
                                   @Param("excludeId") Long excludeId);

    // Trả về danh sách chuyến bay xung đột tại sân bay khởi hành
    @Query("SELECT f FROM Flight f WHERE f.departureAirport.airportId = :airportId AND ((f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime)) AND (:excludeId = -1 OR f.flightId != :excludeId)")
    List<Flight> findDepartureAirportConflicts(@Param("airportId") Long airportId,
                                               @Param("departureTime") LocalDateTime departureTime,
                                               @Param("arrivalTime") LocalDateTime arrivalTime,
                                               @Param("excludeId") Long excludeId);

    // Trả về danh sách chuyến bay xung đột tại sân bay hạ cánh
    @Query("SELECT f FROM Flight f WHERE f.arrivalAirport.airportId = :airportId AND ((f.departureTime < :arrivalTime AND f.arrivalTime > :departureTime)) AND (:excludeId = -1 OR f.flightId != :excludeId)")
    List<Flight> findArrivalAirportConflicts(@Param("airportId") Long airportId,
                                             @Param("departureTime") LocalDateTime departureTime,
                                             @Param("arrivalTime") LocalDateTime arrivalTime,
                                             @Param("excludeId") Long excludeId);

    // Lấy giá thấp nhất theo tuyến và ngày (batch query)
    @Query("SELECT f.departureAirport.airportId, f.arrivalAirport.airportId, DATE(f.departureTime), MIN(tc.price) FROM Flight f JOIN f.flightTravelClasses tc WHERE f.departureAirport.airportId IN :departureAirportIds AND f.arrivalAirport.airportId IN :arrivalAirportIds AND DATE(f.departureTime) IN :dates GROUP BY f.departureAirport.airportId, f.arrivalAirport.airportId, DATE(f.departureTime)")
    List<Object[]> findMinPriceByRouteAndDates(@Param("departureAirportIds") List<Long> departureAirportIds,
                                               @Param("arrivalAirportIds") List<Long> arrivalAirportIds,
                                               @Param("dates") List<java.time.LocalDate> dates);

    // Lấy các cặp chuyến bay khứ hồi theo groupId, ngày đi/về và giá từng chiều
    @Query("SELECT f1.roundTripGroupId, DATE(f1.departureTime), DATE(f2.departureTime), MIN(tc1.price), MIN(tc2.price) " +
            "FROM Flight f1 JOIN Flight f2 ON f1.roundTripGroupId = f2.roundTripGroupId " +
            "AND f1.tripType = 'ROUND_TRIP' AND f2.tripType = 'ROUND_TRIP' " +
            "AND f1.departureAirport.airportId = :depAirportId AND f1.arrivalAirport.airportId = :arrAirportId " +
            "AND f2.departureAirport.airportId = :arrAirportId AND f2.arrivalAirport.airportId = :depAirportId " +
            "AND DATE(f1.departureTime) IN :outboundDates AND DATE(f2.departureTime) IN :returnDates " +
            "JOIN f1.flightTravelClasses tc1 JOIN f2.flightTravelClasses tc2 " +
            "GROUP BY f1.roundTripGroupId, DATE(f1.departureTime), DATE(f2.departureTime)")
    List<Object[]> findRoundTripMinPrices(@Param("depAirportId") Long depAirportId,
                                          @Param("arrAirportId") Long arrAirportId,
                                          @Param("outboundDates") List<java.time.LocalDate> outboundDates,
                                          @Param("returnDates") List<java.time.LocalDate> returnDates);

    @Query("SELECT f FROM Flight f WHERE f.departureTime < :departureTime AND f.status != :status")
    List<Flight> findFlightsByDepartureTimeBeforeAndStatusNot(@Param("departureTime") LocalDateTime departureTime,
                                                              @Param("status") FlightStatus status);

    @Query("SELECT f FROM Flight f WHERE f.departureTime < :departureTime AND f.status = :status")
    List<Flight> findFlightsByDepartureTimeBeforeAndStatus(@Param("departureTime") LocalDateTime departureTime,
                                                           @Param("status") FlightStatus status);

    @Query("SELECT f FROM Flight f WHERE f.departureTime BETWEEN :startTime AND :endTime AND f.status = :status")
    List<Flight> findFlightsByDepartureTimeBetweenAndStatus(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("status") FlightStatus status);

//     @Query("SELECT f FROM Flight f LEFT JOIN FETCH f.aircraft LEFT JOIN FETCH f.flightTravelClasses ftc LEFT JOIN FETCH ftc.travelClass WHERE f.flightId = :flightId")
    @Query("SELECT DISTINCT f FROM Flight f LEFT JOIN FETCH f.aircraft LEFT JOIN FETCH f.flightTravelClasses ftc LEFT JOIN FETCH ftc.travelClass WHERE f.flightId = :flightId")
    Optional<Flight> findByIdWithAircraftAndTravelClasses(@Param("flightId") Long flightId);

    @Query("SELECT f.departureAirport.airportId, f.arrivalAirport.airportId, FUNCTION('DATE', f.departureTime), MIN(f.basePrice) " +
            "FROM Flight f " +
            "WHERE f.departureAirport.airportId IN :departureAirportIds " +
            "AND f.arrivalAirport.airportId IN :arrivalAirportIds " +
            "AND FUNCTION('DATE', f.departureTime) IN :dates " +
            "AND f.departureTime > :minDepartureTime " +
            "AND f.status <> iuh.fit.airsky.enums.FlightStatus.CANCELLED " +
            "GROUP BY f.departureAirport.airportId, f.arrivalAirport.airportId, FUNCTION('DATE', f.departureTime)")
    List<Object[]> findMinPriceByRouteAndDatesWithMinDepartureTime(
            @Param("departureAirportIds") List<Long> departureAirportIds,
            @Param("arrivalAirportIds") List<Long> arrivalAirportIds,
            @Param("dates") List<java.time.LocalDate> dates,
            @Param("minDepartureTime") java.time.LocalDateTime minDepartureTime
    );

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Flight f " +
            "WHERE f.aircraft.aircraftId = :aircraftId AND f.status = iuh.fit.airsky.enums.FlightStatus.ON_TIME")
    boolean existsOnTimeFlightByAircraftId(@Param("aircraftId") Long aircraftId);
}
