package iuh.fit.airsky.dto.request;

import iuh.fit.airsky.enums.LoyaltyTier;
import iuh.fit.airsky.enums.Role;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequest {

    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be a valid number (10-15 digits)")
    private String phone;

    @Size(max = 255, message = "Avatar URL must not exceed 255 characters")
    private String avatar;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Passport number must not exceed 20 characters")
    private String passportNumber;

    @Future(message = "Passport expiry must be in the future")
    private LocalDate passportExpiry;

    @Min(value = 0, message = "Loyalty points must be non-negative")
    private Integer loyaltyPoints;

    private LoyaltyTier loyaltyTier;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private Boolean isVerified; // Chỉ admin sửa
    private Boolean active;   // Chỉ admin sửa
    private Role role;      // Chỉ admin sửa
}