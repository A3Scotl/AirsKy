package iuh.fit.airsky.repository;

import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.FlightStatus;
import iuh.fit.airsky.model.Booking;
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
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.flight LEFT JOIN FETCH b.travelClass LEFT JOIN FETCH b.payment")
    Page<Booking> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"checkIns","checkIns.baggage"})
    List<Booking> findByStatus(BookingStatus bookingStatus);

    @Override
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.userId LEFT JOIN FETCH b.flight LEFT JOIN FETCH b.travelClass LEFT JOIN FETCH b.payment WHERE b.bookingId = :id")
    Optional<Booking> findById(@Param("id") Long id);




    List<Booking> findByUserId(User user);

    @Query("SELECT b FROM Booking b WHERE b.flight.status = :flightStatus AND b.status = :bookingStatus")
    List<Booking> findBookingsByFlightStatusAndBookingStatus(FlightStatus flightStatus, BookingStatus bookingStatus);

    @Query("SELECT b FROM Booking b JOIN b.passengers p LEFT JOIN FETCH b.userId LEFT JOIN FETCH b.flight LEFT JOIN FETCH b.travelClass LEFT JOIN FETCH b.payment LEFT JOIN FETCH b.passengers WHERE b.bookingCode = :bookingCode AND CONCAT(LOWER(p.firstName), ' ', LOWER(p.lastName)) = LOWER(:fullName)")
    Optional<Booking> findByBookingCodeAndPassengerFullName(@Param("bookingCode") String bookingCode, @Param("fullName") String fullName);

    // Method để fetch passengers riêng
    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.passengers p LEFT JOIN FETCH p.seat WHERE b.bookingId = :bookingId")
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

}