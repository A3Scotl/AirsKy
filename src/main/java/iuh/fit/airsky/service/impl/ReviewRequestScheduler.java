package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewRequestScheduler {

    private final ReviewService reviewService;

    /**
     * Tự động tạo review request cho các chuyến bay đã hoàn thành
     * Chạy mỗi 30 phút
     */
    @Scheduled(fixedRate = 1800000) // 30 phút = 30 * 60 * 1000 ms
    public void createReviewRequestsForCompletedFlights() {
        log.info("Scheduled task: Creating review requests for completed flights");
        try {
            reviewService.createReviewRequestsForCompletedFlights();
        } catch (Exception e) {
            log.error("Error in scheduled review request creation", e);
        }
    }
}