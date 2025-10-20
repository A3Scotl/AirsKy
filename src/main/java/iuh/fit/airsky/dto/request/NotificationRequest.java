package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {
    // userId là optional - null cho SYSTEM_ANNOUNCEMENT (broadcast cho tất cả)
    private Long userId;
    
    private String title;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    private Long relatedId;
    private Boolean isRead;

}