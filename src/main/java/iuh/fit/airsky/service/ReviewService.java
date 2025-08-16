package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.ReviewRequest;
import iuh.fit.airsky.dto.response.ReviewResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewService {
    ReviewResponse createReview(ReviewRequest request);
    ReviewResponse updateReview(Long id, ReviewRequest request);
    Optional<ReviewResponse> findById(Long id);
    PageResponse<ReviewResponse> findAll(Pageable pageable);
    void delete(Long id);
}