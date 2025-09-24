package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.flight.flightId = :flightId AND r.isApproved = true")
    List<Review> findByFlightIdAndIsApprovedTrue(@Param("flightId") Long flightId);

    List<Review> findByUserId(Long userId);

    @Query("SELECT r FROM Review r WHERE r.booking.bookingId = :bookingId")
    List<Review> findByBookingId(@Param("bookingId") Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.flight.flightId = :flightId AND r.isApproved = true")
    Double findAverageRatingByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.booking.bookingId = :bookingId AND r.user.id = :userId")
    boolean existsByBookingIdAndUserId(@Param("bookingId") Long bookingId, @Param("userId") Long userId);
}