package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.NotificationRequest;
import iuh.fit.airsky.dto.response.NotificationResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("notificationService")
public interface NotificationService {
    void sendNotificationToUser(Long userId, String type, String message);
    void sendNotificationToUserWithRelatedId(Long userId, String type, String message, Long relatedId);
    PageResponse<NotificationResponse> findByUserId(Long userId, Pageable pageable);
    List<NotificationResponse> findUnreadByUserId(Long userId);
    Long getUnreadCountByUserId(Long userId);
    void markAsRead(Long userId, List<Long> notificationIds);
    void markAllAsRead(Long userId);

    // Các phương thức bổ sung để khớp với Controller
    NotificationResponse createNotification(NotificationRequest request);
    NotificationResponse updateNotification(Long id, NotificationRequest request);
    Optional<NotificationResponse> findById(Long id);
    PageResponse<NotificationResponse> findAll(Pageable pageable);
    void softDelete(Long id);
    Long getNotificationUserId(Long notificationId);
    void broadcastSystemNotification(String type, String title, String message, Long relatedId);
}