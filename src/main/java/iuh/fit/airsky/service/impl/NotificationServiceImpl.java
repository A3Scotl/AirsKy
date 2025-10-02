package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.NotificationRequest;
import iuh.fit.airsky.dto.response.NotificationResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.NotificationMapper;
import iuh.fit.airsky.model.Notification;
import iuh.fit.airsky.repository.NotificationRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, NotificationMapper notificationMapper, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.userRepository = userRepository;
    }

    @Override
    public NotificationResponse createNotification(NotificationRequest request) {
        log.info("Creating new notification for user ID: {}", request.getUserId());
        Notification notification = notificationMapper.toEntity(request);
        notification.setUser(userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + request.getUserId())));
        Notification saved = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", saved.getNotificationId());
        return notificationMapper.toResponseDTO(saved);
    }

    @Override
    public NotificationResponse updateNotification(Long id, NotificationRequest request) {
        log.info("Updating notification with ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setType(request.getType());
        notification.setRelatedId(request.getRelatedId());
        Notification updated = notificationRepository.save(notification);
        log.info("Notification updated with ID: {}", updated.getNotificationId());
        return notificationMapper.toResponseDTO(updated);
    }

    @Override
    public Optional<NotificationResponse> findById(Long id) {
        log.info("Finding notification by ID: {}", id);
        return notificationRepository.findById(id).map(notificationMapper::toResponseDTO);
    }

    @Override
    public PageResponse<NotificationResponse> findAll(Pageable pageable) {
        log.info("Finding all notifications with pagination: {}", pageable);
        Page<Notification> page = notificationRepository.findAll(pageable);
        return new PageResponse<>(page.map(notificationMapper::toResponseDTO));
    }

    @Override
    public PageResponse<NotificationResponse> findByUserId(Long userId, Pageable pageable) {
        log.info("Finding notifications for user ID: {} with pagination: {}", userId, pageable);
        Page<Notification> page = notificationRepository.findByUserId(userId, pageable);
        return new PageResponse<>(page.map(notificationMapper::toResponseDTO));
    }

    @Override
    public List<NotificationResponse> findUnreadByUserId(Long userId) {
        log.info("Finding unread notifications for user ID: {}", userId);
        List<Notification> notifications = notificationRepository.findUnreadByUserId(userId);
        return notifications.stream().map(notificationMapper::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long userId, List<Long> notificationIds) {
        log.info("Marking notifications as read for user ID: {} with IDs: {}", userId, notificationIds);
        notificationRepository.markAsReadByUserIdAndIds(userId, notificationIds);
    }

    @Override
    public void softDelete(Long id) {
        log.info("Soft deleting notification with ID: {}", id);
        if (notificationRepository.findById(id).isEmpty()) {
            log.warn("Notification not found for soft delete: {}", id);
            throw new ResourceNotFoundException("Notification not found with id " + id);
        }
        notificationRepository.softDeleteById(id, LocalDateTime.now());
        log.info("Notification soft deleted: {}", id);
    }
}