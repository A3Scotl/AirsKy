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
    List<ReviewResponse> findByRoute(String departureCode, String arrivalCode);
    List<ReviewResponse> findByUserId(Long userId);
    List<ReviewResponse> findByBookingId(Long bookingId);
    List<ReviewResponse> findByBookingFlightId(Long bookingId);
    ReviewResponse findReviewByBookingAndUser(Long bookingId, Long userId);
    void hideReview(Long id);
    boolean hasUserReviewedBooking(Long bookingId, Long userId);

    // Tự động tạo review request cho các booking đã hoàn thành chuyến bay
    void createReviewRequestsForCompletedFlights();

    // Lấy danh sách review requests đang chờ
    List<ReviewResponse> findPendingReviewRequests();

    // Lấy review requests pending của một user
    List<ReviewResponse> findPendingReviewRequestsByUser(Long userId);

    // Gửi email mời review cho các review request pending
    void sendPendingReviewEmails();

    // Retry gửi email cho các review request thất bại
    void retryFailedReviewEmails();
}
