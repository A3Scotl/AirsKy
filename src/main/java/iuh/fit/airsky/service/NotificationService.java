package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.NotificationRequest;
import iuh.fit.airsky.dto.response.NotificationResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    NotificationResponse createNotification(NotificationRequest request);
    NotificationResponse updateNotification(Long id, NotificationRequest request);
    Optional<NotificationResponse> findById(Long id);
    PageResponse<NotificationResponse> findAll(Pageable pageable);
    PageResponse<NotificationResponse> findByUserId(Long userId, Pageable pageable);
    List<NotificationResponse> findUnreadByUserId(Long userId);
    void markAsRead(Long userId, List<Long> notificationIds);
    void softDelete(Long id);
}