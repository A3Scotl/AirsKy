package iuh.fit.airsky.service;

import iuh.fit.airsky.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupService {

    private final NotificationService notificationService;

    /**
     * Tự động cleanup notifications đã đọc cũ hơn 30 ngày
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldReadNotifications() {
        try {
            int deletedCount = notificationService.cleanupOldReadNotifications(30);
            if (deletedCount > 0) {
                log.info("Scheduled cleanup completed: deleted {} old read notifications", deletedCount);
            }
        } catch (Exception e) {
            log.error("Scheduled cleanup failed: {}", e.getMessage(), e);
        }
    }
}