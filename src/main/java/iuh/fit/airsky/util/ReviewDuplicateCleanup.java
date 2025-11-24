package iuh.fit.airsky.util;

import iuh.fit.airsky.model.Review;
import iuh.fit.airsky.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class to clean up duplicate reviews.
 * This should be run once to fix existing data integrity issues.
 * Enable by setting: app.cleanup.reviews.enabled=true
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewDuplicateCleanup implements CommandLineRunner {

    private final ReviewRepository reviewRepository;
    
    @Value("${app.cleanup.reviews.enabled:false}")
    private boolean cleanupEnabled;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!cleanupEnabled) {
            log.debug("Review duplicate cleanup is disabled. Set app.cleanup.reviews.enabled=true to enable.");
            return;
        }
        
        log.info("Starting review duplicate cleanup...");
        
        // Find all active reviews
        List<Review> allReviews = reviewRepository.findAll().stream()
                .filter(r -> r.getDeletedAt() == null)
                .collect(Collectors.toList());
        
        // Group by booking and user
        Map<String, List<Review>> groupedReviews = allReviews.stream()
                .collect(Collectors.groupingBy(review -> 
                    review.getBooking().getBookingId() + "_" + review.getUser().getId()));
        
        int duplicatesFound = 0;
        int duplicatesRemoved = 0;
        
        for (Map.Entry<String, List<Review>> entry : groupedReviews.entrySet()) {
            List<Review> reviews = entry.getValue();
            if (reviews.size() > 1) {
                duplicatesFound += reviews.size() - 1;
                log.info("Found {} duplicate reviews for booking-user: {}", reviews.size() - 1, entry.getKey());
                
                // Sort by creation date descending and keep the latest
                reviews.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
                
                // Soft delete all except the first (latest) one
                for (int i = 1; i < reviews.size(); i++) {
                    Review duplicateReview = reviews.get(i);
                    duplicateReview.setDeletedAt(LocalDateTime.now());
                    duplicateReview.setUpdatedAt(LocalDateTime.now());
                    reviewRepository.save(duplicateReview);
                    duplicatesRemoved++;
                    log.debug("Soft deleted duplicate review with ID: {}", duplicateReview.getReviewId());
                }
            }
        }
        
        if (duplicatesFound > 0) {
            log.info("Review duplicate cleanup completed. Found {} duplicates, removed {}", duplicatesFound, duplicatesRemoved);
        } else {
            log.info("No duplicate reviews found. Database is clean.");
        }
    }
}