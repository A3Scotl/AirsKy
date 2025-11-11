package iuh.fit.airsky.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    @Email
    @NotBlank private String email;
    @NotBlank private String otpCode;
    @NotBlank private String newPassword;
}