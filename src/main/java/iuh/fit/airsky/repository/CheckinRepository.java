package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.CheckIn;
import iuh.fit.airsky.model.FlightSegment;
import iuh.fit.airsky.model.Passenger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<CheckIn, Long> {

    @Query("SELECT c FROM CheckIn c WHERE c.deleted = false")
    Page<CheckIn> findAll(Pageable pageable);

    @Query("SELECT c FROM CheckIn c WHERE c.checkInId = :id AND c.deleted = false")
    Optional<CheckIn> findById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE CheckIn c SET c.deleted = true, c.deletedAt = :now, c.active = false WHERE c.checkInId = :id")
    void softDeleteById(Long id, LocalDateTime now);

    @Query("SELECT c FROM CheckIn c LEFT JOIN FETCH c.baggage WHERE c.booking.bookingId = :bookingId AND c.deleted = false")
    List<CheckIn> findByBookingIdWithBaggage(@Param("bookingId") Long bookingId);

    @Query("SELECT c FROM CheckIn c WHERE c.booking.bookingId = :bookingId AND c.deleted = false")
    List<CheckIn> findByBookingId(@Param("bookingId") Long bookingId);

    @Modifying
    @Transactional
    @Query("UPDATE CheckIn c SET c.deleted = true, c.deletedAt = :now, c.active = false WHERE c.passenger = :passenger")
    void deleteByPassenger(Passenger passenger, LocalDateTime now);

    @Query("SELECT COUNT(c) > 0 FROM CheckIn c WHERE c.passenger = :passenger AND c.status = 'COMPLETED' AND c.deleted = false")
    boolean existsByPassengerAndCompleted(@Param("passenger") Passenger passenger);

    @Query("SELECT c FROM CheckIn c WHERE c.booking.bookingCode = :bookingCode AND c.passenger.passengerId = :passengerId AND c.deleted = false")
    Optional<CheckIn> findByBookingCodeAndPassengerId(@Param("bookingCode") String bookingCode, @Param("passengerId") Long passengerId);

    // Segment-specific check-in queries
    @Query("SELECT COUNT(c) > 0 FROM CheckIn c WHERE c.passenger = :passenger AND c.flightSegment = :segment AND c.status = 'COMPLETED' AND c.deleted = false")
    boolean existsByPassengerAndSegmentAndCompleted(@Param("passenger") Passenger passenger, @Param("segment") FlightSegment segment);

    @Query("SELECT c FROM CheckIn c WHERE c.passenger = :passenger AND c.flightSegment = :segment AND c.deleted = false")
    Optional<CheckIn> findByPassengerAndSegment(@Param("passenger") Passenger passenger, @Param("segment") FlightSegment segment);

    @Query("SELECT c FROM CheckIn c LEFT JOIN FETCH c.baggage WHERE c.passenger = :passenger AND c.deleted = false ORDER BY c.flightSegment.segmentOrder")
    List<CheckIn> findByPassengerWithSegments(@Param("passenger") Passenger passenger);

    

    @Query("SELECT COUNT(c) > 0 FROM CheckIn c WHERE c.passenger = :passenger AND c.booking = :booking AND c.deleted = false")
    boolean existsByPassengerAndBooking(@Param("passenger") Passenger passenger, @Param("booking") Booking booking);

    @Query("SELECT c FROM CheckIn c WHERE c.status = 'PENDING' AND c.flightSegment.flight.departureTime < :now AND c.deleted = false")
    List<CheckIn> findPendingCheckinsForDepartedFlights(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM CheckIn c WHERE c.booking.status = 'CANCELLED' AND c.status != 'CANCELLED' AND c.status != 'COMPLETED' AND c.deleted = false")
    List<CheckIn> findCheckinsForCancelledBookings();

    @Query("SELECT c FROM CheckIn c WHERE c.booking = :booking AND c.flightSegment = :segment AND c.deleted = false")
    Optional<CheckIn> findByBookingAndSegment(@Param("booking") Booking booking, @Param("segment") FlightSegment segment);

}