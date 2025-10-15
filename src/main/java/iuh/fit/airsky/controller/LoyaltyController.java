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

    /**
     * Lấy thông tin loyalty stats của user hiện tại
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getLoyaltyStats(Authentication authentication) {
        log.info("Getting loyalty stats for user: {}", authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        Map<String, Object> stats = loyaltyService.getLoyaltyStats(userId);

        return ResponseEntity.ok(stats);
    }

    /**
     * Kiểm tra và nâng hạng loyalty cho user hiện tại
     */
    @PostMapping("/check-upgrade")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> checkAndUpgradeTier(Authentication authentication) {
        log.info("Checking tier upgrade for user: {}", authentication.getName());

        Long userId = getUserIdFromAuthentication(authentication);
        loyaltyService.checkAndUpgradeTier(userId);

        // Trả về stats sau khi upgrade
        Map<String, Object> stats = loyaltyService.getLoyaltyStats(userId);
        return ResponseEntity.ok(stats);
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

    /**
     * Helper method để lấy userId từ Authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        // Assuming the principal is a User object with getId() method
        // You may need to adjust this based on your UserDetails implementation
        if (authentication.getPrincipal() instanceof iuh.fit.airsky.model.User) {
            return ((iuh.fit.airsky.model.User) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
}