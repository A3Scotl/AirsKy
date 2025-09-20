package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.LoyaltyTier;
import iuh.fit.airsky.enums.Role;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private boolean isVerified = true;
    private Role role;
    private boolean active = true;
    private LocalDate dateOfBirth;
    private String avatar;
    private String passportNumber;
    private LocalDate passportExpiry;
    private Integer loyaltyPoints;
    private LoyaltyTier loyaltyTier;
}