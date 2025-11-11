package iuh.fit.airsky.controller;

import iuh.fit.airsky.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
@Slf4j
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final iuh.fit.airsky.service.UserService userService;

    /**
     * Lấy thông tin loyalty stats của user hiện tại
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLoyaltyStats(Authentication authentication) {
        try {
            String email = authentication.getName();
            var userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy người dùng với email: " + email,
                    "error", "USER_NOT_FOUND"
                ));
            }
            Long userId = userOpt.get().getId();
            Map<String, Object> stats = loyaltyService.getLoyaltyStats(userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
        } catch (Exception e) {
            log.error("Lỗi lấy loyalty stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An unexpected error occurred: " + e.getMessage(),
                "error", "UNEXPECTED_ERROR"
            ));
        }
    }

    /**
     * Kiểm tra và nâng hạng loyalty cho user hiện tại
     */
    @PostMapping("/check-upgrade")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkAndUpgradeTier(Authentication authentication) {
        try {
            String email = authentication.getName();
            var userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Không tìm thấy người dùng với email: " + email,
                    "error", "USER_NOT_FOUND"
                ));
            }
            Long userId = userOpt.get().getId();
            loyaltyService.checkAndUpgradeTier(userId);
            Map<String, Object> stats = loyaltyService.getLoyaltyStats(userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
        } catch (Exception e) {
            log.error("Lỗi nâng hạng loyalty: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An unexpected error occurred: " + e.getMessage(),
                "error", "UNEXPECTED_ERROR"
            ));
        }
    }

    /**
     * API admin để kiểm tra nâng hạng cho một user cụ thể
     */
    @PostMapping("/admin/check-upgrade/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminCheckUpgrade(@PathVariable Long userId) {
        log.info("Admin checking tier upgrade for user: {}", userId);

        try {
            loyaltyService.checkAndUpgradeTier(userId);
            return ResponseEntity.ok("Tier upgrade check completed for user: " + userId);
        } catch (Exception e) {
            log.error("Failed to check tier upgrade for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Failed to check tier upgrade: " + e.getMessage());
        }
    }

    /**
     * API admin để lấy loyalty stats của một user cụ thể
     */
    @GetMapping("/admin/stats/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminGetLoyaltyStats(@PathVariable Long userId) {
        log.info("Admin getting loyalty stats for user: {}", userId);

        try {
            Map<String, Object> stats = loyaltyService.getLoyaltyStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get loyalty stats for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get loyalty stats: " + e.getMessage()));
        }
    }
}