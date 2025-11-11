package iuh.fit.airsky.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank @Email private String email;
    @NotBlank private String oldPassword;
    @NotBlank private String newPassword;
}