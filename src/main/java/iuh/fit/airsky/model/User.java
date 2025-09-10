package iuh.fit.airsky.model;

import iuh.fit.airsky.base.BaseFullSoftDeleteEntity;
import iuh.fit.airsky.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User  extends BaseFullSoftDeleteEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;

    // `unique = true` cũng sẽ yêu cầu database tạo một unique constraint, thường đi kèm index
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String phone;
    private boolean isVerified = false;
    private String businessName;

    @Enumerated(EnumType.STRING)
    private Role role;

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }
    private LocalDateTime lastLogin;

    @Override
    public String getUsername() {
        return email;
    }
    public String getDisplayName() {
        if (role == Role.BUSINESS && businessName != null) {
            return businessName;
        }
        return firstName + " " + lastName;
    }

    // Các phương thức còn lại của UserDetails
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return isActive(); }
}