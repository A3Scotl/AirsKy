package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.NotificationRequest;
import iuh.fit.airsky.dto.response.NotificationResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.enums.NotificationType;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.NotificationMapper;
import iuh.fit.airsky.model.Notification;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.NotificationRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    @Async // Chạy bất đồng bộ để không làm chậm các tiến trình chính (đặt vé, thanh toán)
    public void sendNotificationToUser(Long userId, String type, String message) {
        sendNotificationToUserWithRelatedId(userId, type, message, null);
    }

    @Override
    @Transactional
    @Async // Chạy bất đồng bộ để không làm chậm các tiến trình chính (đặt vé, thanh toán)
    public void sendNotificationToUserWithRelatedId(Long userId, String type, String message, Long relatedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        try {
            NotificationType notificationType = NotificationType.valueOf(type);

            // 1. Tạo và lưu notification vào database
            Notification notification = Notification.builder()
                    .user(user)
                    .title(notificationType.getDefaultTitle())
                    .message(message)
                    .type(notificationType)
                    .relatedId(relatedId)
                    .isRead(false)
                    .build();
            Notification savedNotification = notificationRepository.save(notification);
            log.info("Saved notification {} for user {}", savedNotification.getNotificationId(), userId);

            // 2. Chuyển đổi sang DTO
            NotificationResponse response = notificationMapper.toResponse(savedNotification);

            // 3. Gửi thông báo qua WebSocket đến user cụ thể
            // Client sẽ lắng nghe trên "/user/queue/notifications"
            String destination = "/queue/notifications";
            messagingTemplate.convertAndSendToUser(user.getId().toString(), destination, Map.of(
                "notificationId", savedNotification.getNotificationId(),
                "title", savedNotification.getTitle(),
                "message", savedNotification.getMessage(),
                "type", savedNotification.getType().toString(),
                "relatedId", savedNotification.getRelatedId(),
                "isRead", savedNotification.getIsRead(),
                "timestamp", savedNotification.getCreatedAt()
            ));

            log.info("Sent notification to user {} via WebSocket on destination {}", userId, destination);

        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> findByUserId(Long userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findByUserId(userId, pageable);
        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(
                responses,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> findUnreadByUserId(Long userId) {
        List<Notification> notifications = notificationRepository.findUnreadByUserId(userId);
        return notifications.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCountByUserId(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        notificationRepository.markAsReadByUserIdAndIds(userId, notificationIds);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // Các phương thức dưới đây để tương thích với Controller, bạn có thể triển khai logic chi tiết hơn nếu cần

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        Notification notification = Notification.builder()
                .user(user)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .isRead(false)
                .relatedId(request.getRelatedId())
                .build();
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public NotificationResponse updateNotification(Long id, NotificationRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        // Cập nhật các trường từ request
        if (request.getTitle() != null) {
            notification.setTitle(request.getTitle());
        }
        if (request.getMessage() != null) {
            notification.setMessage(request.getMessage());
        }
        if (request.getType() != null) {
            notification.setType(request.getType());
        }
        if (request.getIsRead() != null) {
            notification.setIsRead(request.getIsRead());
        }

        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationResponse> findById(Long id) {
        return notificationRepository.findById(id).map(notificationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> findAll(Pageable pageable) {
        Page<Notification> notificationPage = notificationRepository.findAll(pageable);
        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(responses, notificationPage.getNumber(), notificationPage.getSize(), notificationPage.getTotalElements(), notificationPage.getTotalPages(), notificationPage.isLast());
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        notificationRepository.softDeleteById(id, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getNotificationUserId(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        return notification.getUser().getId();
    }

    @Override
    @Transactional
    public void broadcastSystemNotification(String type, String title, String message, Long relatedId) {
        try {
            // Tạo notification mẫu để lấy title từ enum
            NotificationType notificationType = NotificationType.valueOf(type);
            String finalTitle = title != null ? title : notificationType.getDefaultTitle();

            // Gửi broadcast qua WebSocket tới tất cả client đang kết nối
            messagingTemplate.convertAndSend("/topic/system-notifications", Map.of(
                "title", finalTitle,
                "message", message,
                "type", type,
                "relatedId", relatedId,
                "timestamp", LocalDateTime.now(),
                "broadcast", true
            ));

            log.info("Broadcast system notification to all users: {} - {}", finalTitle, message);

        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
        } catch (Exception e) {
            log.error("Failed to broadcast system notification: {}", e.getMessage(), e);
        }
    }
}