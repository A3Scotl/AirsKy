package iuh.fit.airsky.security;

import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.NotificationRepository;
import iuh.fit.airsky.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component("securityService")
public class SecurityService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public SecurityService(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Kiểm tra xem authentication có tương ứng với userId được cung cấp hay không.
     * Hỗ trợ khi principal là domain User, UserDetails, hoặc String (email).
     */
    public boolean isSelf(Authentication authentication, Long userId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;

        Object principal = authentication.getPrincipal();
        String email = null;

        try {
            if (principal instanceof iuh.fit.airsky.model.User) {
                Long id = ((iuh.fit.airsky.model.User) principal).getId();
                return id != null && id.equals(userId);
            }

            // Nếu principal là UserDetails (từ JWT authentication), lấy username (email)
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            }
            // Nếu principal là String (username từ JWT)
            else if (principal instanceof String) {
                email = (String) principal;
            }

            // Tìm user theo email và so sánh userId
            if (email != null) {
                return userRepository.findByEmail(email)
                    .map(user -> user.getId().equals(userId))
                    .orElse(false);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy ID của user hiện tại từ authentication.
     */
    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;

        Object principal = authentication.getPrincipal();
        String email = null;

        try {
            if (principal instanceof UserDetails) {
                email = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            }

            if (email != null) {
                return userRepository.findByEmail(email)
                    .map(User::getId)
                    .orElse(null);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
