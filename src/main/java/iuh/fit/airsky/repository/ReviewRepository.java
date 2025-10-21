package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.flight.flightId = :flightId AND r.isApproved = true")
    List<Review> findByFlightIdAndIsApprovedTrue(@Param("flightId") Long flightId);

    @Query("SELECT r FROM Review r WHERE r.flight.departureAirport.airportCode = :departureCode AND r.flight.arrivalAirport.airportCode = :arrivalCode AND r.isApproved = true")
    List<Review> findByRouteAndIsApprovedTrue(@Param("departureCode") String departureCode, @Param("arrivalCode") String arrivalCode);

    List<Review> findByUserId(Long userId);

    @Query("SELECT r FROM Review r WHERE r.booking.bookingId = :bookingId")
    List<Review> findByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.flight.flightId = :flightId AND r.isApproved = true")
    Double findAverageRatingByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT r FROM Review r WHERE r.booking.bookingId = :bookingId AND r.user.id = :userId")
    Optional<Review> findByBookingIdAndUserId(@Param("bookingId") Long bookingId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.booking.bookingId = :bookingId AND r.user.id = :userId")
    boolean existsByBookingIdAndUserId(@Param("bookingId") Long bookingId, @Param("userId") Long userId);


    @Query("SELECT DISTINCT b.bookingId, b.userId.id, fs.flight.flightId " +
           "FROM Booking b " +
           "JOIN b.flightSegments fs " +
           "WHERE fs.flight.arrivalTime < CURRENT_TIMESTAMP " +
           "AND fs.flight.status = 'DEPARTED' " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.userId.id IS NOT NULL " +
           "AND NOT EXISTS (SELECT r FROM Review r WHERE r.booking.bookingId = b.bookingId)")
    List<Object[]> findCompletedBookingsWithoutReviewRequests();

    @Query("SELECT r FROM Review r WHERE r.status = 'PENDING'")
    List<Review> findPendingReviewRequests();

    List<Review> findByStatus(Review.ReviewStatus status);

    @Query("SELECT r FROM Review r WHERE r.user.id = :userId AND r.status = :status")
    List<Review> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Review.ReviewStatus status);
}
