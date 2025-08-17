package iuh.fit.airsky.dto.response;

import iuh.fit.airsky.enums.Role;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private boolean isVerified;
    private Role role;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private LocalDateTime deletedAt;
    private boolean deleted;
}