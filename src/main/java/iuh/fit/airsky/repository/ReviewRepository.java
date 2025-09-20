package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByFlightIdAndIsApprovedTrue(Long flightId);

    List<Review> findByUserId(Long userId);

    List<Review> findByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.flightId = :flightId AND r.isApproved = true")
    Double findAverageRatingByFlightId(@Param("flightId") Long flightId);

    boolean existsByBookingIdAndUserId(Long bookingId, Long userId);
}