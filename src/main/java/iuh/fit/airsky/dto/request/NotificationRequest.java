package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.NotificationType;
import lombok.Data;

@Data
public class NotificationRequest {
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private Long relatedId;
    private Boolean isRead;

}