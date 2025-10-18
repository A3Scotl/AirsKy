package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.NotificationRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.NotificationResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.security.JwtUtil;
import iuh.fit.airsky.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(@RequestBody NotificationRequest request) {
        log.info("Creating notification: {}", request);
        NotificationResponse response = notificationService.createNotification(request);

        // Gửi real-time notification qua WebSocket
            notificationService.sendNotificationToUserWithRelatedId(
                request.getUserId(),
                request.getType().name(),
                request.getMessage(),
                request.getRelatedId()
            );

        ApiResponse<NotificationResponse> apiResponse = new ApiResponse<>(true, "Notification created successfully", response, null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> broadcastNotification(@RequestBody NotificationRequest request) {
        log.info("Broadcasting notification to all customers: {}", request);

        try {
            // Gửi thông báo hệ thống cho tất cả customer
            notificationService.broadcastSystemNotification(
                request.getType().name(),
                request.getTitle(),
                request.getMessage(),
                request.getRelatedId()
            );

            ApiResponse<String> apiResponse = new ApiResponse<>(
                true,
                "Notification broadcasted to all customers successfully",
                "Broadcast completed",
                null, null, null
            );
            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Failed to broadcast notification: {}", e.getMessage(), e);
            ApiResponse<String> apiResponse = new ApiResponse<>(
                false,
                "Failed to broadcast notification",
                null, null, null, null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> updateNotification(@PathVariable Long id, @RequestBody NotificationRequest request) {
        log.info("Updating notification with ID: {}", id);
        NotificationResponse response = notificationService.updateNotification(id, request);
        ApiResponse<NotificationResponse> apiResponse = new ApiResponse<>(true, "Notification updated successfully", response, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(@PathVariable Long id) {
        log.info("Getting notification by ID: {}", id);
        Optional<NotificationResponse> response = notificationService.findById(id);
        if (response.isPresent()) {
            ApiResponse<NotificationResponse> apiResponse = new ApiResponse<>(true, "Notification found", response.get(), null, null, null);
            return ResponseEntity.ok(apiResponse);
        } else {
            ApiResponse<NotificationResponse> apiResponse = new ApiResponse<>(false, "Notification not found", null, null, null, null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting all notifications with page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> response = notificationService.findAll(pageable);
        ApiResponse<PageResponse<NotificationResponse>> apiResponse = new ApiResponse<>(true, "Notifications retrieved successfully", response, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.getCurrentUserId(authentication) == #userId")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotificationsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting notifications for user ID: {} with page: {}, size: {}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<NotificationResponse> response = notificationService.findByUserId(userId, pageable);
        ApiResponse<PageResponse<NotificationResponse>> apiResponse = new ApiResponse<>(true, "Notifications retrieved successfully", response, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/user/{userId}/count-unread")
    @PreAuthorize("@securityService.getCurrentUserId(authentication) == #userId")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount(@PathVariable Long userId) {
        log.info("Getting unread notification count for user ID: {}", userId);
        Long count = notificationService.getUnreadCountByUserId(userId);
        ApiResponse<Long> apiResponse = new ApiResponse<>(true, "Unread notification count retrieved successfully", count, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/user/{userId}/mark-read")
    @PreAuthorize("@securityService.getCurrentUserId(authentication) == #userId")
    public ResponseEntity<ApiResponse<Void>> markNotificationsAsRead(@PathVariable Long userId, @RequestBody List<Long> notificationIds) {
        log.info("Marking notifications as read for user ID: {} with IDs: {}", userId, notificationIds);
        notificationService.markAsRead(userId, notificationIds);
        ApiResponse<Void> apiResponse = new ApiResponse<>(true, "Notifications marked as read successfully", null, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Đánh dấu tất cả notification của user là đã đọc
     */
    @PutMapping("/user/{userId}/mark-read-all")
    @PreAuthorize("@securityService.getCurrentUserId(authentication) == #userId")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead(@PathVariable Long userId) {
        log.info("Marking ALL notifications as read for user ID: {}", userId);
        notificationService.markAllAsRead(userId);
        ApiResponse<Void> apiResponse = new ApiResponse<>(true, "All notifications marked as read successfully", null, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isSelf(authentication, @notificationService.getNotificationUserId(#id))")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        log.info("Deleting notification with ID: {}", id);
        notificationService.softDelete(id);
        ApiResponse<Void> apiResponse = new ApiResponse<>(true, "Notification deleted successfully", null, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }
}