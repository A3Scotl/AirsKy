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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    private final ApplicationEventPublisher eventPublisher;


    /**
     * Unified method to create and send notification (replaces sendNotificationToUser, sendNotificationToUserWithRelatedId, createNotification, sendNotification)
     * @param userId User ID, null for SYSTEM_ANNOUNCEMENT
     * @param type Notification type (e.g., "BOOKING_CONFIRMED", "SYSTEM_ANNOUNCEMENT")
     * @param message Notification message
     * @param relatedId Related entity ID, can be null
     * @param title Custom title, can be null to use default from NotificationType
     * @return NotificationResponse
     */
    @Override
    public NotificationResponse createAndSendNotification(Long userId, String type, String message, Long relatedId, String title) {
        try {
            log.info("🚀 Starting createAndSendNotification: userId={}, type={}, message={}, relatedId={}, title={}",
                     userId, type, message, relatedId, title);

            NotificationType notificationType = null;
            try {
                notificationType = type != null ? NotificationType.valueOf(type) : null;
                log.info("✅ NotificationType parsed: {}", notificationType);
            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid notification type: {}", type, e);
                throw new IllegalArgumentException("Invalid notification type: " + type, e);
            }

            // Validate userId requirement
            // if (notificationType != NotificationType.SYSTEM_ANNOUNCEMENT && userId == null) {
            //     log.error("❌ UserId is required for notification type: {}", notificationType);
            //     throw new IllegalArgumentException("UserId is required for notification type: " + notificationType);
            // }

            User user = null;
            if (notificationType != NotificationType.SYSTEM_ANNOUNCEMENT && userId != null) {
                log.info("👤 Looking up user with ID: {}", userId);
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
                log.info("👤 Found user: {} (ID: {})", user.getEmail(), user.getId());
            } else {
                log.info("📡 System announcement or null userId - no user lookup needed");
            }

            log.info("🏗️ Building notification object...");
            Notification notification = Notification.builder()
                    .user(user)
                    .title(title != null ? title : (notificationType != null ? notificationType.getDefaultTitle() : "Thông báo"))
                    .message(message)
                    .type(notificationType)
                    .relatedId(relatedId)
                    .isRead(false)
                    .build();

            log.info("💾 Saving notification to database...");
            try {
                Notification savedNotification = notificationRepository.save(notification);
                log.info("💾 Saved notification {} for user {}", savedNotification.getNotificationId(), userId);

                // Send WebSocket notification immediately after saving
                log.info("📡 Sending WebSocket notification immediately for notification {}", savedNotification.getNotificationId());
                sendWebSocketNotification(savedNotification);

                if (savedNotification.getType() != NotificationType.SYSTEM_ANNOUNCEMENT && savedNotification.getUser() != null) {
                    log.info("👤 Sending user-specific WebSocket notification to user {}", savedNotification.getUser().getId());
                    sendUserWebSocketNotification(savedNotification);
                }

                return notificationMapper.toResponse(savedNotification);
            } catch (Exception saveException) {
                log.error("❌ Failed to save notification to database: {} (Exception type: {})", saveException.getMessage(), saveException.getClass().getSimpleName(), saveException);
                throw new RuntimeException("Failed to save notification: " + (saveException.getMessage() != null ? saveException.getMessage() : saveException.getClass().getSimpleName()), saveException);
            }
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid notification type: {}", type, e);
            throw new IllegalArgumentException("Invalid notification type: " + type, e);
        } catch (Exception e) {
            log.error("❌ Failed to create notification for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create notification: " + e.getMessage(), e);
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
        return notificationRepository.findUnreadByUserId(userId).stream()
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
        if (notificationIds != null && !notificationIds.isEmpty()) {
            notificationRepository.markAsReadByUserIdAndIds(userId, notificationIds);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private void sendWebSocketNotification(Notification notification) {
        if (notification == null) {
            log.error("❌ Cannot send WebSocket notification: notification is null");
            return;
        }
        if (messagingTemplate == null) {
            log.error("❌ Cannot send WebSocket notification: messagingTemplate is null");
            return;
        }

        log.info("📡 Preparing to send WebSocket notification for notification {}", notification.getNotificationId());

        if (notification.getType() == NotificationType.SYSTEM_ANNOUNCEMENT) {
            Map<String, Object> broadcastData = new HashMap<>();
            broadcastData.put("notificationId", notification.getNotificationId());
            broadcastData.put("title", notification.getTitle());
            broadcastData.put("message", notification.getMessage());
            broadcastData.put("type", notification.getType() != null ? notification.getType().toString() : "UNKNOWN");
            broadcastData.put("relatedId", notification.getRelatedId()); // Can be null
            broadcastData.put("isRead", notification.getIsRead());
            broadcastData.put("timestamp", notification.getCreatedAt());
            broadcastData.put("broadcast", true);

            log.info("📡 Sending broadcast to /topic/notifications: {}", broadcastData);
            messagingTemplate.convertAndSend("/topic/notifications", broadcastData);
            log.info("✅ Sent broadcast WebSocket notification {} for system announcement", notification.getNotificationId());
        } else {
            log.info("📡 Notification {} of type {} created - no broadcast sent", notification.getNotificationId(), notification.getType());
        }
    }

    private void sendUserWebSocketNotification(Notification notification) {
        if (notification == null || notification.getUser() == null) {
            log.error("❌ Cannot send user-specific WebSocket notification: notification or user is null");
            return;
        }
        if (messagingTemplate == null) {
            log.error("❌ Cannot send user-specific WebSocket notification: messagingTemplate is null");
            return;
        }

        log.info("👤 Preparing user-specific WebSocket notification for user {}", notification.getUser().getId());

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("notificationId", notification.getNotificationId());
        notificationData.put("title", notification.getTitle());
        notificationData.put("message", notification.getMessage());
        notificationData.put("type", notification.getType() != null ? notification.getType().toString() : "UNKNOWN");
        notificationData.put("relatedId", notification.getRelatedId()); // Can be null
        notificationData.put("isRead", notification.getIsRead());
        notificationData.put("timestamp", notification.getCreatedAt());

        String destination = "/queue/notifications";
        log.info("👤 Sending to user {} on destination {} with data: {}", notification.getUser().getId(), destination, notificationData);
        try {
            messagingTemplate.convertAndSendToUser(notification.getUser().getId().toString(), destination, notificationData);
            log.info("✅ Sent user-specific WebSocket notification {} to user {} on destination {}",
                     notification.getNotificationId(), notification.getUser().getId(), destination);
        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket message to user {}: {}", notification.getUser().getId(), e.getMessage(), e);
            // Don't throw exception here to avoid rolling back the notification save
        }
    }

    @Override
    @Transactional
    public NotificationResponse updateNotification(Long id, NotificationRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        Optional.ofNullable(request.getTitle()).ifPresent(notification::setTitle);
        Optional.ofNullable(request.getMessage()).ifPresent(notification::setMessage);
        Optional.ofNullable(request.getType()).ifPresent(notification::setType);
        Optional.ofNullable(request.getIsRead()).ifPresent(notification::setIsRead);

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
        return new PageResponse<>(responses, notificationPage.getNumber(), notificationPage.getSize(), 
                                 notificationPage.getTotalElements(), notificationPage.getTotalPages(), notificationPage.isLast());
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
        return notification.getUser() != null ? notification.getUser().getId() : null;
    }

    @Override
    @Transactional
    public void broadcastSystemNotification(String type, String title, String message, Long relatedId) {
        try {
            NotificationType notificationType = NotificationType.valueOf(type);
            String finalTitle = title != null ? title : notificationType.getDefaultTitle();

            Map<String, Object> broadcastData = new HashMap<>();
            broadcastData.put("title", finalTitle);
            broadcastData.put("message", message);
            broadcastData.put("type", type);
            broadcastData.put("relatedId", relatedId); // Can be null
            broadcastData.put("timestamp", LocalDateTime.now());
            broadcastData.put("broadcast", true);

            messagingTemplate.convertAndSend("/topic/notifications", broadcastData);
            log.info("Broadcast system notification to all users: {} - {}", finalTitle, message);
        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type, e);
        } catch (Exception e) {
            log.error("Failed to broadcast system notification: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int cleanupOldReadNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = notificationRepository.cleanupOldReadNotifications(cutoffDate, LocalDateTime.now());
        log.info("Cleaned up {} old read notifications older than {} days", deletedCount, daysOld);
        return deletedCount;
    }

    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public void handleNotificationCreated(NotificationCreatedEvent event) {
    //     Notification notification = event.getNotification();
    //     log.info("🔥 Handling NotificationCreatedEvent for notification {} after commit", notification.getNotificationId());
    //     log.info("🔥 Notification details: userId={}, type={}, message={}",
    //              notification.getUser() != null ? notification.getUser().getId() : "null",
    //              notification.getType(), notification.getMessage());

    //     sendWebSocketNotification(notification);

    //     if (notification.getType() != NotificationType.SYSTEM_ANNOUNCEMENT && notification.getUser() != null) {
    //         log.info("🔥 Sending user-specific WebSocket notification to user {}", notification.getUser().getId());
    //         sendUserWebSocketNotification(notification);
    //     } else {
    //         log.info("🔥 Skipping user-specific notification: type={}, user={}",
    //                  notification.getType(), notification.getUser());
    //     }
    // }
}