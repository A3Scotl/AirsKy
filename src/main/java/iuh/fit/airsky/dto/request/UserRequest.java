package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.Role;
import lombok.Data;

@Data
public class UserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private boolean isVerified = true;
    private Role role;
}