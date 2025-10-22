package iuh.fit.airsky.repository;

import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.flight LEFT JOIN FETCH b.travelClass LEFT JOIN FETCH b.payment ORDER BY b.createdAt DESC")
    Page<Booking> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"checkIns","checkIns.baggage"})
    List<Booking> findByStatus(BookingStatus bookingStatus);

    @Override
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.userId LEFT JOIN FETCH b.flight LEFT JOIN FETCH b.travelClass LEFT JOIN FETCH b.payment WHERE b.bookingId = :id")
    Optional<Booking> findById(@Param("id") Long id);

    
    List<Booking> findByFlightAndStatus(Flight flight, BookingStatus status);



    List<Booking> findByUserId(User user);

    @Query("SELECT b FROM Booking b WHERE b.flight.status = :flightStatus AND b.status = :bookingStatus")
    List<Booking> findBookingsByFlightStatusAndBookingStatus(FlightStatus flightStatus, BookingStatus bookingStatus);

    @Query("SELECT b FROM Booking b JOIN b.passengers p WHERE b.bookingCode = :bookingCode AND CONCAT(p.firstName, ' ', p.lastName) = :fullName")
    Optional<Booking> findByBookingCodeAndPassengerFullName(@Param("bookingCode") String bookingCode, @Param("fullName") String fullName);

    @Query("SELECT b FROM Booking b JOIN b.passengers p WHERE b.bookingCode = :bookingCode AND p.passengerId = :passengerId")
    Optional<Booking> findByBookingCodeAndPassengerId(@Param("bookingCode") String bookingCode, @Param("passengerId") Long passengerId);

    // Method để fetch passengers riêng
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers p LEFT JOIN FETCH p.seatAssignments sa LEFT JOIN FETCH sa.seat WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithPassengers(@Param("bookingId") Long bookingId);

    // Method để fetch flight segments riêng  
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.flightSegments WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithFlightSegments(@Param("bookingId") Long bookingId);

    // Method để fetch check-ins riêng
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.checkIns c LEFT JOIN FETCH c.baggage LEFT JOIN FETCH c.passenger WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithCheckIns(@Param("bookingId") Long bookingId);

    // Method để tìm các booking đã hết thời hạn thanh toán
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.userId LEFT JOIN FETCH b.flight LEFT JOIN FETCH b.payment WHERE b.paymentTimeout < :now AND b.status = :status")
    List<Booking> findExpiredBookings(@Param("now") java.time.LocalDateTime now, @Param("status") BookingStatus status);

    // Method để tìm các booking hết hạn gần đây
    @Query("SELECT b FROM Booking b WHERE b.paymentTimeout < :now AND b.status = :status AND b.createdAt > :lookbackTime")
    List<Booking> findRecentExpiredBookings(@Param("now") java.time.LocalDateTime now, @Param("status") BookingStatus status, @Param("lookbackTime") java.time.LocalDateTime lookbackTime);

    // Method để đếm số booking hoàn thành theo user
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.userId.id = :userId AND b.status = :status")
    Long countCompletedBookingsByUser(@Param("userId") Long userId, @Param("status") BookingStatus status);

    // Method để tìm bookings cho thông báo check-in
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.userId " +
           "LEFT JOIN FETCH b.flight f " +
           "LEFT JOIN FETCH b.payment p " +
           "WHERE f.departureTime BETWEEN :startTime AND :endTime " +
           "AND b.status = :bookingStatus " +
           "AND p.status = :paymentStatus " +
           "AND b.userId IS NOT NULL")
    List<Booking> findBookingsForCheckinNotifications(
        @Param("startTime") java.time.LocalDateTime startTime,
        @Param("endTime") java.time.LocalDateTime endTime,
        @Param("bookingStatus") BookingStatus bookingStatus,
        @Param("paymentStatus") PaymentStatus paymentStatus
    );

}