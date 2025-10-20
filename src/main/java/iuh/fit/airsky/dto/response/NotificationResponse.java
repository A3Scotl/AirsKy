package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private Long relatedId;
    private LocalDateTime createdAt;
}