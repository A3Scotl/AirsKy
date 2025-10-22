package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.NotificationType;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.ReviewMapper;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Flight;
import iuh.fit.airsky.model.Review;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.FlightRepository;
import iuh.fit.airsky.repository.ReviewRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ReviewMapper reviewMapper,
                           UserRepository userRepository, BookingRepository bookingRepository,
                           FlightRepository flightRepository, EmailService emailService,
                           NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        log.info("Creating review for booking ID: {}", request.getBookingId());

        // Fetch required entities
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + request.getFlightId()));

        // Validate user permission - user chỉ có thể review booking của chính họ
        if (!booking.getUserId().getId().equals(request.getUserId())) {
            throw new IllegalArgumentException("User can only review their own bookings");
        }

        // Validate flight đã hoàn thành (đã bay xong)
        if (flight.getArrivalTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot review flight that hasn't completed yet");
        }

        // Validate booking đã được confirm
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only review confirmed or completed bookings");
        }

        // Tìm review request đã có hoặc tạo mới
        // Điều này ngăn việc tạo nhiều review request cho cùng một booking
        Review review = reviewRepository.findByBookingIdAndUserId(request.getBookingId(), request.getUserId())
                .orElseGet(() -> {
                    Review newReview = new Review();
                    newReview.setUser(user);
                    newReview.setBooking(booking);
                    newReview.setFlight(flight);
                    newReview.setStatus(Review.ReviewStatus.PENDING);
                    newReview.setRetryCount(0);
                    newReview.setRating(0); 
                    newReview.setIsApproved(false); // Review requests are not approved until user submits
                    newReview.setEligibleAt(request.getEligibleAt() != null ? request.getEligibleAt() : LocalDateTime.now());
                    return newReview;
                });

        // Nếu có rating thì đây là review thực sự, ngược lại là review request
        if (request.getRating() != null) {
            // User đang submit review
            if (review.getStatus() == Review.ReviewStatus.COMPLETED) {
                throw new IllegalArgumentException("User has already reviewed this booking");
            }
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setReviewDate(request.getReviewDate() != null ? request.getReviewDate() : LocalDateTime.now());
            review.setStatus(Review.ReviewStatus.COMPLETED); // Review thực sự
            review.setIsApproved(true); // User reviews are automatically approved, admin can hide if needed
        } else {
            // Đây là trường hợp tạo review request tự động từ scheduler
            // Không cần làm gì thêm vì đã xử lý ở orElseGet
        }

        Review savedReview = reviewRepository.save(review);

        // Gửi notification khi user submit review
        if (request.getRating() != null) {
            notificationService.createAndSendNotification(
                request.getUserId(),
                NotificationType.REVIEW_SUBMITTED.toString(),
                "Cảm ơn bạn đã đánh giá chuyến bay " + flight.getFlightNumber() + " với " + request.getRating() + " sao.",
                savedReview.getReviewId(),
                "Đánh giá của bạn đã được gửi"
            );
        }

        return reviewMapper.toResponseDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long id, ReviewRequest request) {
        log.info("Updating review with ID: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        boolean approvalChanged = false;
        boolean wasApproved = review.getIsApproved() != null ? review.getIsApproved() : false;
        boolean newApproval = request.getIsApproved() != null ? request.getIsApproved() : wasApproved;

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }
        if (request.getIsApproved() != null) {
            review.setIsApproved(request.getIsApproved());
            approvalChanged = !request.getIsApproved().equals(wasApproved);
        }

        Review updatedReview = reviewRepository.save(review);

        // Gửi notification khi approval status thay đổi
        if (approvalChanged) {
            NotificationType notificationType = newApproval ? NotificationType.REVIEW_APPROVED : NotificationType.REVIEW_HIDDEN;
            String title = newApproval ? "Đánh giá của bạn đã được duyệt" : "Đánh giá của bạn đã bị ẩn";
            String message = newApproval ?
                "Đánh giá của bạn cho chuyến bay " + review.getFlight().getFlightNumber() + " đã được duyệt và hiển thị công khai." :
                "Đánh giá của bạn cho chuyến bay " + review.getFlight().getFlightNumber() + " đã bị ẩn theo chính sách của hệ thống.";

            notificationService.createAndSendNotification(
                review.getUser().getId(),
                notificationType.toString(),
                message,
                updatedReview.getReviewId(),
                title
            );
        }

        return reviewMapper.toResponseDTO(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        log.info("Deleting review with ID: {}", id);
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Review not found");
        }
        reviewRepository.deleteById(id);
    }

    @Override
    public ReviewResponse findById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        return reviewMapper.toResponseDTO(review);
    }

    @Override
    public PageResponse<ReviewResponse> findAll(Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAll(pageable);
        List<ReviewResponse> content = reviews.getContent().stream()
                .map(reviewMapper::toResponseDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(),
                reviews.getTotalElements(), reviews.getTotalPages(), reviews.isLast());
    }

    @Override
    public List<ReviewResponse> findByFlightId(Long flightId) {
        List<Review> reviews = reviewRepository.findByFlightIdAndIsApprovedTrue(flightId);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> findByRoute(String departureCode, String arrivalCode) {
        List<Review> reviews = reviewRepository.findByRouteAndIsApprovedTrue(departureCode, arrivalCode);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> findByUserId(Long userId) {
        List<Review> reviews = reviewRepository.findByUserId(userId);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> findByBookingId(Long bookingId) {
        List<Review> reviews = reviewRepository.findByBookingId(bookingId);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponse> findByBookingFlightId(Long bookingId) {
        // Lấy booking để tìm flightId
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // Lấy flightId từ booking (có thể có nhiều flight segments, lấy flight đầu tiên)
        Long flightId = booking.getFlight().getFlightId();

        // Tìm reviews theo flightId
        List<Review> reviews = reviewRepository.findByFlightIdAndIsApprovedTrue(flightId);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse findReviewByBookingAndUser(Long bookingId, Long userId) {
        Optional<Review> review = reviewRepository.findByBookingIdAndUserId(bookingId, userId);
        return review.map(reviewMapper::toResponseDTO).orElse(null);
    }

    @Override
    @Transactional
    public void hideReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setIsApproved(false); // Admin can hide reviews by setting isApproved to false
        reviewRepository.save(review);
    }

    @Override
    public boolean hasUserReviewedBooking(Long bookingId, Long userId) {
        return reviewRepository.existsByBookingIdAndUserId(bookingId, userId);
    }

    @Override
    @Transactional
    @Async
    public void createReviewRequestsForCompletedFlights() {
        log.info("Checking for completed flights to create review requests...");

        List<Object[]> completedBookings = reviewRepository.findCompletedBookingsWithoutReviewRequests();

        for (Object[] bookingData : completedBookings) {
            Long bookingId = (Long) bookingData[0];
            Long userId = (Long) bookingData[1];
            Long flightId = (Long) bookingData[2];

            try {
                // Tạo ReviewRequest cho review request
                ReviewRequest reviewRequest = new ReviewRequest();
                reviewRequest.setBookingId(bookingId);
                reviewRequest.setUserId(userId);
                reviewRequest.setFlightId(flightId);
                reviewRequest.setEligibleAt(LocalDateTime.now().plusHours(24)); // Đủ điều kiện review sau 24h
                reviewRequest.setIsApproved(false);
                // Rating và comment để null vì đây là review request

                ReviewResponse savedReview = createReview(reviewRequest);
                log.info("Created review request for booking ID: {}, user ID: {}, flight ID: {}", bookingId, userId, flightId);

                // Lấy thông tin flight để tạo notification
                Flight flight = flightRepository.findById(flightId)
                    .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + flightId));

                // Gửi notification cho user về review request
                notificationService.createAndSendNotification(
                    userId,
                    NotificationType.REVIEW_REQUEST_CREATED.toString(),
                    "Chuyến bay của bạn đã hoàn thành. Hãy dành chút thời gian để đánh giá trải nghiệm của bạn.",
                    savedReview.getReviewId(),
                    "Hãy đánh giá chuyến bay " + flight.getFlightNumber()
                );

            } catch (Exception e) {
                log.error("Error creating review request for booking ID: {}, user ID: {}, flight ID: {}", bookingId, userId, flightId, e);
            }
        }

        log.info("Finished creating review requests. Processed {} completed bookings.", completedBookings.size());
    }

    @Override
    public List<ReviewResponse> findPendingReviewRequests() {
        log.info("Finding pending review requests...");
        List<Review> pendingReviews = reviewRepository.findPendingReviewRequests();
        return reviewMapper.toResponseDTOList(pendingReviews);
    }

    @Override
    public List<ReviewResponse> findPendingReviewRequestsByUser(Long userId) {
        log.info("Finding pending review requests for user: {}", userId);
        List<Review> pendingReviews = reviewRepository.findByUserIdAndStatus(userId, Review.ReviewStatus.PENDING);
        return reviewMapper.toResponseDTOList(pendingReviews);
    }

    @Override
    @Transactional
    public void sendPendingReviewEmails() {
        log.info("Sending pending review emails...");

        List<Review> pendingReviews = reviewRepository.findPendingReviewRequests();
        int successCount = 0;
        int failureCount = 0;

        for (Review review : pendingReviews) {
            try {
                // Kiểm tra xem đã đủ thời gian chờ chưa (24h sau khi eligible)
                if (review.getEligibleAt().isAfter(LocalDateTime.now().minusHours(24))) {
                    log.debug("Review {} not yet eligible for email (eligible at: {})", review.getReviewId(), review.getEligibleAt());
                    continue;
                }

                // Gửi email mời review
                String userEmail = review.getUser().getEmail();
                String userName = review.getUser().getFirstName() + " " + review.getUser().getLastName();
                String flightNumber = review.getFlight().getFlightNumber();

                emailService.sendReviewInvitationEmail(userEmail, userName, review.getBooking().getBookingId(), flightNumber);

                // Cập nhật status và sentAt
                review.setStatus(Review.ReviewStatus.SENT);
                review.setSentAt(LocalDateTime.now());
                reviewRepository.save(review);

                successCount++;
                log.info("Sent review invitation email for review ID: {}", review.getReviewId());

            } catch (Exception e) {
                log.error("Failed to send review email for review ID: {}", review.getReviewId(), e);

                // Cập nhật status failed và tăng retry count
                review.setStatus(Review.ReviewStatus.FAILED);
                review.setRetryCount(review.getRetryCount() != null ? review.getRetryCount() + 1 : 1);
                review.setLastError(e.getMessage());
                reviewRepository.save(review);

                failureCount++;
            }
        }

        log.info("Finished sending review emails. Success: {}, Failed: {}", successCount, failureCount);
    }

    @Override
    @Transactional
    public void retryFailedReviewEmails() {
        log.info("Retrying failed review emails...");

        List<Review> failedReviews = reviewRepository.findByStatus(Review.ReviewStatus.FAILED);
        int successCount = 0;
        int permanentFailureCount = 0;

        for (Review review : failedReviews) {
            // Chỉ retry nếu chưa vượt quá 3 lần
            if (review.getRetryCount() != null && review.getRetryCount() >= 3) {
                log.debug("Review {} has exceeded max retry attempts ({})", review.getReviewId(), review.getRetryCount());
                permanentFailureCount++;
                continue;
            }

            try {
                String userEmail = review.getUser().getEmail();
                String userName = review.getUser().getFirstName() + " " + review.getUser().getLastName();
                String flightNumber = review.getFlight().getFlightNumber();

                emailService.sendReviewInvitationEmail(userEmail, userName, review.getBooking().getBookingId(), flightNumber);

                // Reset error và cập nhật status
                review.setStatus(Review.ReviewStatus.SENT);
                review.setSentAt(LocalDateTime.now());
                review.setLastError(null);
                reviewRepository.save(review);

                successCount++;
                log.info("Successfully retried review email for review ID: {}", review.getReviewId());

            } catch (Exception e) {
                log.error("Retry failed for review ID: {}", review.getReviewId(), e);

                review.setRetryCount(review.getRetryCount() != null ? review.getRetryCount() + 1 : 1);
                review.setLastError(e.getMessage());
                reviewRepository.save(review);
            }
        }

        log.info("Finished retrying failed review emails. Success: {}, Permanent failures: {}", successCount, permanentFailureCount);
    }
}