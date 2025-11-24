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
        
        // Find all active reviews (not soft deleted)
        List<Review> allReviews = reviewRepository.findAll().stream()
                .filter(r -> !r.isDeleted()) // is_deleted = 0
                .filter(r -> r.getDeletedAt() == null) // deleted_at is NULL
                .collect(Collectors.toList());
        
        log.info("Found {} active reviews to check for duplicates", allReviews.size());
        
        // Group by booking_id and user_id
        Map<String, List<Review>> groupedReviews = allReviews.stream()
                .collect(Collectors.groupingBy(review -> 
                    review.getBooking().getBookingId() + "_" + review.getUser().getId()));
        
        int duplicatesFound = 0;
        int duplicatesRemoved = 0;
        
        for (Map.Entry<String, List<Review>> entry : groupedReviews.entrySet()) {
            List<Review> reviews = entry.getValue();
            if (reviews.size() > 1) {
                duplicatesFound += reviews.size() - 1;
                log.info("Found {} duplicate reviews for booking-user: {} (review_ids: {})", 
                        reviews.size() - 1, 
                        entry.getKey(),
                        reviews.stream().map(r -> r.getReviewId().toString()).collect(Collectors.joining(", ")));
                
                // Sort by review_id ascending to keep the oldest (smallest ID)
                reviews.sort((r1, r2) -> r1.getReviewId().compareTo(r2.getReviewId()));
                
                // Soft delete all except the first (oldest) one
                for (int i = 1; i < reviews.size(); i++) {
                    Review duplicateReview = reviews.get(i);
                    
                    // Use soft delete method from base class
                    duplicateReview.softDelete();
                    duplicateReview.setUpdatedAt(LocalDateTime.now()); // updated_at timestamp
                    
                    reviewRepository.save(duplicateReview);
                    duplicatesRemoved++;
                    
                    log.info("Soft deleted duplicate review: ID={}, booking_id={}, user_id={}, status={}", 
                            duplicateReview.getReviewId(),
                            duplicateReview.getBooking().getBookingId(),
                            duplicateReview.getUser().getId(),
                            duplicateReview.getStatus());
                }
                
                // Log the kept review
                Review keptReview = reviews.get(0);
                log.info("Kept review: ID={}, booking_id={}, user_id={}, status={}, rating={}", 
                        keptReview.getReviewId(),
                        keptReview.getBooking().getBookingId(), 
                        keptReview.getUser().getId(),
                        keptReview.getStatus(),
                        keptReview.getRating());
            }
        }
        
        if (duplicatesFound > 0) {
            log.info("Review duplicate cleanup completed. Found {} duplicates, removed {}", duplicatesFound, duplicatesRemoved);
            
            // Log summary statistics
            logCleanupSummary();
        } else {
            log.info("No duplicate reviews found. Database is clean.");
        }
    }
    
    private void logCleanupSummary() {
        // Count reviews by status after cleanup
        List<Review> activeReviews = reviewRepository.findAll().stream()
                .filter(r -> !r.isDeleted())
                .collect(Collectors.toList());
                
        Map<Review.ReviewStatus, Long> statusCounts = activeReviews.stream()
                .collect(Collectors.groupingBy(Review::getStatus, Collectors.counting()));
        
        log.info("=== CLEANUP SUMMARY ===");
        log.info("Active reviews after cleanup: {}", activeReviews.size());
        statusCounts.forEach((status, count) -> 
            log.info("  {}: {}", status, count));
        log.info("=======================");
    }
}