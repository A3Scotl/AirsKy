package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.NotificationRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.NotificationResponse;
import iuh.fit.airsky.dto.response.PageResponse;
import iuh.fit.airsky.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(@RequestBody NotificationRequest request) {
        log.info("Creating notification: {}", request);
        NotificationResponse response = notificationService.createNotification(request);
        ApiResponse<NotificationResponse> apiResponse = new ApiResponse<>(true, "Notification created successfully", response, null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationResponse>> updateNotification(@PathVariable Long id, @RequestBody NotificationRequest request) {
        log.info("Updating notification with ID: {}", id);
        NotificationResponse response = notificationService.updateNotification(id, request);
        ApiResponse<NotificationResponse> apiResponse = new ApiResponse<>(true, "Notification updated successfully", response, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
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

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotificationsByUserId(@PathVariable Long userId) {
        log.info("Getting unread notifications for user ID: {}", userId);
        List<NotificationResponse> response = notificationService.findUnreadByUserId(userId);
        ApiResponse<List<NotificationResponse>> apiResponse = new ApiResponse<>(true, "Unread notifications retrieved successfully", response, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/user/{userId}/mark-read")
    public ResponseEntity<ApiResponse<Void>> markNotificationsAsRead(@PathVariable Long userId, @RequestBody List<Long> notificationIds) {
        log.info("Marking notifications as read for user ID: {} with IDs: {}", userId, notificationIds);
        notificationService.markAsRead(userId, notificationIds);
        ApiResponse<Void> apiResponse = new ApiResponse<>(true, "Notifications marked as read successfully", null, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        log.info("Deleting notification with ID: {}", id);
        notificationService.softDelete(id);
        ApiResponse<Void> apiResponse = new ApiResponse<>(true, "Notification deleted successfully", null, null, null, null);
        return ResponseEntity.ok(apiResponse);
    }
}