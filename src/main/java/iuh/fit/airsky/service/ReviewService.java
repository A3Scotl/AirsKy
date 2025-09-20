package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(ReviewRequest request);
    ReviewResponse updateReview(Long id, ReviewRequest request);
    void deleteReview(Long id);
    ReviewResponse findById(Long id);
    PageResponse<ReviewResponse> findAll(Pageable pageable);
    List<ReviewResponse> findByFlightId(Long flightId);
    List<ReviewResponse> findByUserId(Long userId);
    Double getAverageRatingByFlightId(Long flightId);
    void approveReview(Long id);
    boolean hasUserReviewedBooking(Long bookingId, Long userId);
}