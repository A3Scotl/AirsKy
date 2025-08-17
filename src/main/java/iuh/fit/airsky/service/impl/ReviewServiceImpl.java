package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.ReviewMapper;
import iuh.fit.airsky.model.Review;
import iuh.fit.airsky.repository.FlightRepository;
import iuh.fit.airsky.repository.ReviewRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;
    private final FlightRepository flightRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ReviewMapper reviewMapper, UserRepository userRepository, FlightRepository flightRepository) {
        this.reviewRepository = reviewRepository;
        this.reviewMapper = reviewMapper;
        this.userRepository = userRepository;
        this.flightRepository = flightRepository;
    }

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        log.info("Creating new review for flight ID: {}", request.getFlightId());
        Review review = reviewMapper.toEntity(request);
        review.setUser(userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.getUserId())));
        review.setFlight(flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id " + request.getFlightId())));
        Review saved = reviewRepository.save(review);
        log.info("Review created with ID: {}", saved.getReviewId());
        return reviewMapper.toResponseDTO(saved);
    }

    @Override
    public ReviewResponse updateReview(Long id, ReviewRequest request) {
        log.info("Updating review with ID: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + id));
        review.setRating(request.getRating());
        review.setComments(request.getComments());
        review.setReviewDate(request.getReviewDate());
        Review updated = reviewRepository.save(review);
        log.info("Review updated with ID: {}", updated.getReviewId());
        return reviewMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<ReviewResponse> findById(Long id) {
        log.info("Finding review by ID: {}", id);
        return reviewRepository.findById(id).map(reviewMapper::toResponseDTO);
    }

    @Override
    public PageResponse<ReviewResponse> findAll(Pageable pageable) {
        log.info("Finding all reviews with pagination: {}", pageable);
        Page<Review> page = reviewRepository.findAll(pageable);
        return new PageResponse<>(page.map(reviewMapper::toResponseDTO));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting review with ID: {}", id);
        if (!reviewRepository.existsById(id)) {
            log.warn("Review not found for delete: {}", id);
            throw new ResourceNotFoundException("Review not found with id " + id);
        }
        reviewRepository.deleteById(id);
        log.info("Review deleted: {}", id);
    }
}