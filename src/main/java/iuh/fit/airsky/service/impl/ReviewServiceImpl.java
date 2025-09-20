package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.dto.response.ReviewResponse;
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
import iuh.fit.airsky.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ReviewMapper reviewMapper,
                           UserRepository userRepository, BookingRepository bookingRepository,
                           FlightRepository flightRepository) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        log.info("Creating review for booking ID: {}", request.getBookingId());

        // Validate entities exist
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        // Check if user already reviewed this booking
        if (reviewRepository.existsByBookingIdAndUserId(request.getBookingId(), request.getUserId())) {
            throw new IllegalArgumentException("User has already reviewed this booking");
        }

        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setBooking(booking);
        review.setFlight(flight);
        review.setReviewDate(request.getReviewDate() != null ? request.getReviewDate() : LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);
        return reviewMapper.toResponseDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long id, ReviewRequest request) {
        log.info("Updating review with ID: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }
        if (request.getIsApproved() != null) {
            review.setIsApproved(request.getIsApproved());
        }

        Review updatedReview = reviewRepository.save(review);
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
    public List<ReviewResponse> findByUserId(Long userId) {
        List<Review> reviews = reviewRepository.findByUserId(userId);
        return reviews.stream()
                .map(reviewMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Double getAverageRatingByFlightId(Long flightId) {
        return reviewRepository.findAverageRatingByFlightId(flightId);
    }

    @Override
    @Transactional
    public void approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setIsApproved(true);
        reviewRepository.save(review);
    }

    @Override
    public boolean hasUserReviewedBooking(Long bookingId, Long userId) {
        return reviewRepository.existsByBookingIdAndUserId(bookingId, userId);
    }
}