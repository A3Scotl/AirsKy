package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.service.ReviewService;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.util.ApiResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600) // Allow all origins for email submissions
public class ReviewController {

    private final ReviewService reviewService;
    private final EmailService emailService;

    @PostMapping
    // @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@RequestBody ReviewRequest request) {
        ReviewResponse review = reviewService.createReview(request);
        return ApiResponseUtil.buildResponse(true, "Review created successfully", review, "/api/reviews");
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

    // API để lấy reviews theo 2 điểm đi và đến (airport codes)
    @GetMapping("/route")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByRoute(
            @RequestParam String departureCode,
            @RequestParam String arrivalCode) {
        List<ReviewResponse> reviews = reviewService.findByRoute(departureCode, arrivalCode);
        return ApiResponseUtil.buildResponse(true, "Reviews retrieved successfully", reviews, "/api/reviews/route?departureCode=" + departureCode + "&arrivalCode=" + arrivalCode);
    }

    // API để lấy reviews theo chuyến bay của một booking (lấy flightId từ booking)
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER', 'BUSINESS')")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByBookingFlight(@PathVariable Long bookingId) {
        List<ReviewResponse> reviews = reviewService.findByBookingFlightId(bookingId);
        return ApiResponseUtil.buildResponse(true, "Reviews retrieved successfully", reviews, "/api/reviews/booking/" + bookingId);
    }

    // API để lấy review request của user cho một booking cụ thể
    @GetMapping("/booking/{bookingId}/my-review")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER', 'BUSINESS')")
    public ResponseEntity<ApiResponse<ReviewResponse>> getMyReviewForBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {
        ReviewResponse review = reviewService.findReviewByBookingAndUser(bookingId, userId);
        if (review != null) {
            return ApiResponseUtil.buildResponse(true, "Review found", review, "/api/reviews/booking/" + bookingId + "/my-review");
        } else {
            return ApiResponseUtil.buildResponse(true, "No review found", null, "/api/reviews/booking/" + bookingId + "/my-review");
        }
    }

    @PutMapping("/{id}/hide")
    @PreAuthorize("hasAnyRole('ADMIN', 'FLIGHT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> hideReview(@PathVariable Long id) {
        reviewService.hideReview(id);
        return ApiResponseUtil.buildResponse(true, "Review hidden successfully", null, "/api/reviews/" + id + "/hide");
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



    // Simple submit endpoint without token validation (for direct email forms)
    @PostMapping("/submit-simple")
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> submitSimpleReview(
            @RequestParam Long bookingId,
            @RequestParam Long userId, 
            @RequestParam Long flightId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment) {
        
        try {
            log.info("Received simple review submission - bookingId: {}, userId: {}, flightId: {}, rating: {}", 
                    bookingId, userId, flightId, rating);
            
            // Validate required fields
            if (rating == null || rating < 1 || rating > 5) {
                log.warn("Invalid rating: {}", rating);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<html><head><meta charset='UTF-8'></head><body><h1>Đánh giá không hợp lệ</h1><p>Đánh giá phải từ 1 đến 5 sao.</p></body></html>");
            }
            
            // Create review request
            ReviewRequest request = new ReviewRequest();
            request.setBookingId(bookingId);
            request.setUserId(userId);
            request.setFlightId(flightId);
            request.setRating(rating);
            request.setComment(comment);
            
            log.info("Creating review for bookingId: {}, userId: {}", bookingId, userId);
            reviewService.createReview(request);
            
            log.info("Review created successfully for bookingId: {}", bookingId);
            
            // Return simple success page
            String stars = "★".repeat(rating) + "☆".repeat(5-rating);
            String successHtml = "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>Cảm ơn - AirsKy</title></head>" +
                "<body style=\"font-family: Arial; text-align: center; padding: 50px; background: #f8f9fa;\">" +
                "<div style=\"background: white; padding: 30px; border-radius: 10px; max-width: 500px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1);\">" +
                "<h1 style=\"color: #28a745;\">Cảm ơn đánh giá!</h1>" +
                "<p style=\"font-size: 24px; color: #ffd700; margin: 20px 0;\">" + stars + "</p>" +
                "<p>Đánh giá " + rating + " sao của bạn đã được ghi nhận.</p>" +
                (comment != null && !comment.trim().isEmpty() ? "<p style=\"font-style: italic; color: #666;\">\"" + comment + "\"</p>" : "") +
                "<p>Cảm ơn bạn đã tin tưởng AirsKy Airlines!</p>" +
                
                "</div></body></html>";
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(successHtml);
            
        } catch (Exception e) {
            log.error("Error submitting simple review for bookingId: {}, userId: {}", bookingId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("<html><head><meta charset='UTF-8'></head><body><h1>Có lỗi xảy ra</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }
}