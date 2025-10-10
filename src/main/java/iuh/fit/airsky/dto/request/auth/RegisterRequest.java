package iuh.fit.airsky.dto.request.auth;

import iuh.fit.airsky.dto.request.UserRequest;
import iuh.fit.airsky.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String password;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String phone;
    // Optional: allow admin to specify role
    private Role role;
    public UserRequest toUserRequest() {
        UserRequest req = new UserRequest();
        req.setFirstName(firstName);
        req.setLastName(lastName);
        req.setEmail(email);
        req.setPassword(password);
        req.setPhone(phone);
        // Use provided role if present, else default to CUSTOMER
        req.setRole(role != null ? role : Role.CUSTOMER);
        req.setIsVerified(false);
        return req;
    }

}