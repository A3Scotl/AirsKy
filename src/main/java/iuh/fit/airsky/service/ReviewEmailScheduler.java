package iuh.fit.airsky.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewEmailScheduler {

    private final ReviewService reviewService;

    /**
     * Tạo review request cho các chuyến bay đã hoàn thành (chạy mỗi giờ)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void createReviewRequestsForCompletedFlights() {
        log.info("Starting scheduled task: create review requests for completed flights");
        try {
            reviewService.createReviewRequestsForCompletedFlights();
            log.info("Completed scheduled task: create review requests for completed flights");
        } catch (Exception e) {
            log.error("Error in scheduled task: create review requests for completed flights", e);
        }
    }

    /**
     * Gửi email mời review cho các review request pending (chạy mỗi 30 phút)
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void sendPendingReviewEmails() {
        log.info("Starting scheduled task: send pending review emails");
        try {
            reviewService.sendPendingReviewEmails();
            log.info("Completed scheduled task: send pending review emails");
        } catch (Exception e) {
            log.error("Error in scheduled task: send pending review emails", e);
        }
    }

    /**
     * Retry gửi email cho các review request thất bại (chạy mỗi 2 giờ)
     */
    @Scheduled(fixedRate = 7200000) // 2 hours
    public void retryFailedReviewEmails() {
        log.info("Starting scheduled task: retry failed review emails");
        try {
            reviewService.retryFailedReviewEmails();
            log.info("Completed scheduled task: retry failed review emails");
        } catch (Exception e) {
            log.error("Error in scheduled task: retry failed review emails", e);
        }
    }
}