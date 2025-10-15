package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.service.ReviewService;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@RequestBody ReviewRequest request) {
        ReviewResponse review = reviewService.createReview(request);
        return ApiResponseUtil.buildResponse(true, "Review created successfully", review, "/api/reviews");
    }

    // API để submit review từ email link (không cần auth vì đã verify qua email)
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReviewFromEmail(@RequestBody ReviewRequest request) {
        // Validate required fields for email submission
        if (request.getBookingId() == null || request.getUserId() == null || request.getFlightId() == null) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.BAD_REQUEST, "Missing required fields", "VALIDATION_FAILED", "/api/reviews");
        }
        ReviewResponse review = reviewService.createReview(request);
        return ApiResponseUtil.buildResponse(true, "Review submitted successfully", review, "/api/reviews");
    }

    // API để lấy review request của user (để hiển thị form review)
    @GetMapping("/my-review-requests")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviewRequests(
            @RequestParam Long userId) {
        List<ReviewResponse> pendingReviews = reviewService.findPendingReviewRequestsByUser(userId);
        return ApiResponseUtil.buildResponse(true, "Review requests retrieved", pendingReviews, "/api/reviews/my-review-requests");
    }

    // API để lấy reviews đã duyệt công khai theo flight
    @GetMapping("/flight/{flightId}/approved")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getApprovedReviewsByFlight(@PathVariable Long flightId) {
        List<ReviewResponse> reviews = reviewService.findByFlightId(flightId); // Đã filter isApproved = true
        return ApiResponseUtil.buildResponse(true, "Approved reviews retrieved", reviews, "/api/reviews/flight/" + flightId + "/approved");
    }

    // API này không cần thiết - user thường không update review
    // @PutMapping("/{id}")
    // @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS')")
    // public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(@PathVariable Long id, @RequestBody ReviewRequest request) {
    //     ReviewResponse review = reviewService.updateReview(id, request);
    //     return ApiResponseUtil.buildResponse(true, "Review updated successfully", review, "/api/reviews/" + id);
    // }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ApiResponseUtil.buildResponse(true, "Review deleted successfully", null, "/api/reviews/" + id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable Long id) {
        ReviewResponse review = reviewService.findById(id);
        return ApiResponseUtil.buildResponse(true, "Review retrieved successfully", review, "/api/reviews/" + id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<ReviewResponse> reviews = reviewService.findAll(pageable);
        return ApiResponseUtil.buildResponse(true, "Reviews retrieved successfully", reviews, "/api/reviews");
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByFlight(@PathVariable Long flightId) {
        List<ReviewResponse> reviews = reviewService.findByFlightId(flightId);
        return ApiResponseUtil.buildResponse(true, "Reviews retrieved successfully", reviews, "/api/reviews/flight/" + flightId);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByUser(@PathVariable Long userId) {
        List<ReviewResponse> reviews = reviewService.findByUserId(userId);
        return ApiResponseUtil.buildResponse(true, "Reviews retrieved successfully", reviews, "/api/reviews/user/" + userId);
    }

    @GetMapping("/flight/{flightId}/average-rating")
    public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long flightId) {
        Double averageRating = reviewService.getAverageRatingByFlightId(flightId);
        return ApiResponseUtil.buildResponse(true, "Average rating retrieved successfully", averageRating, "/api/reviews/flight/" + flightId + "/average-rating");
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> approveReview(@PathVariable Long id) {
        reviewService.approveReview(id);
        return ApiResponseUtil.buildResponse(true, "Review approved successfully", null, "/api/reviews/" + id + "/approve");
    }

    @GetMapping("/check/{bookingId}/{userId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS')")
    public ResponseEntity<ApiResponse<Boolean>> hasUserReviewedBooking(@PathVariable Long bookingId, @PathVariable Long userId) {
        boolean hasReviewed = reviewService.hasUserReviewedBooking(bookingId, userId);
        return ApiResponseUtil.buildResponse(true, "Check completed", hasReviewed, "/api/reviews/check/" + bookingId + "/" + userId);
    }

    @PostMapping("/create-review-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> createReviewRequestsForCompletedFlights() {
        reviewService.createReviewRequestsForCompletedFlights();
        return ApiResponseUtil.buildResponse(true, "Review requests created for completed flights", null, "/api/reviews/create-review-requests");
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getPendingReviewRequests() {
        // Thêm method này vào service sau
        List<ReviewResponse> pendingReviews = reviewService.findPendingReviewRequests();
        return ApiResponseUtil.buildResponse(true, "Pending review requests retrieved", pendingReviews, "/api/reviews/pending");
    }
}