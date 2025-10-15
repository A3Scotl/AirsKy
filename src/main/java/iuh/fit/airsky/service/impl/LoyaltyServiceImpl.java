package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.LoyaltyTier;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.BookingRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void checkAndUpgradeTier(Long userId) {
        log.info("Checking tier upgrade eligibility for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));

        LoyaltyTier currentTier = user.getLoyaltyTier() != null ? user.getLoyaltyTier() : LoyaltyTier.STANDARD;
        Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;

        // Đếm số booking hoàn thành
        Long completedBookings = bookingRepository.countCompletedBookingsByUser(userId, BookingStatus.CONFIRMED);

        log.info("User {} - Current tier: {}, Points: {}, Completed bookings: {}",
                userId, currentTier, currentPoints, completedBookings);

        LoyaltyTier newTier = calculateUpgradeTier(currentTier, currentPoints, completedBookings);

        if (newTier != null && newTier != currentTier) {
            user.setLoyaltyTier(newTier);
            userRepository.save(user);

            log.info("User {} upgraded from {} to {} tier", userId, currentTier, newTier);
        } else {
            log.debug("User {} does not qualify for tier upgrade. Current: {}, Required for next: {}",
                    userId, currentTier, getNextTierRequirements(currentTier));
        }
    }

    @Override
    public LoyaltyTier getNextTier(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));

        LoyaltyTier currentTier = user.getLoyaltyTier() != null ? user.getLoyaltyTier() : LoyaltyTier.STANDARD;
        Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
        Long completedBookings = bookingRepository.countCompletedBookingsByUser(userId, BookingStatus.CONFIRMED);

        return calculateUpgradeTier(currentTier, currentPoints, completedBookings);
    }

    @Override
    public Map<String, Object> getLoyaltyStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại với ID: " + userId));

        LoyaltyTier currentTier = user.getLoyaltyTier() != null ? user.getLoyaltyTier() : LoyaltyTier.STANDARD;
        Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
        Long completedBookings = bookingRepository.countCompletedBookingsByUser(userId, BookingStatus.CONFIRMED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("currentTier", currentTier);
        stats.put("currentPoints", currentPoints);
        stats.put("completedBookings", completedBookings);
        stats.put("nextTier", getNextTier(userId));

        // Tính progress cho tier tiếp theo
        Map<String, Object> nextTierRequirements = getNextTierRequirements(currentTier);
        if (nextTierRequirements != null) {
            Integer requiredPoints = (Integer) nextTierRequirements.get("points");
            Integer requiredBookings = (Integer) nextTierRequirements.get("bookings");

            double pointsProgress = requiredPoints > 0 ? (double) currentPoints / requiredPoints : 1.0;
            double bookingsProgress = requiredBookings > 0 ? (double) completedBookings / requiredBookings : 1.0;

            stats.put("pointsProgress", Math.min(pointsProgress, 1.0));
            stats.put("bookingsProgress", Math.min(bookingsProgress, 1.0));
            stats.put("overallProgress", Math.min(pointsProgress, bookingsProgress));
            stats.put("nextTierRequirements", nextTierRequirements);
        }

        return stats;
    }

    /**
     * Tính toán tier mới dựa trên points và số booking hoàn thành
     */
    private LoyaltyTier calculateUpgradeTier(LoyaltyTier currentTier, Integer points, Long completedBookings) {
        // Nếu chưa có tier, mặc định là STANDARD
        if (currentTier == null) {
            currentTier = LoyaltyTier.STANDARD;
        }

        // Logic nâng hạng theo thứ tự
        switch (currentTier) {
            case STANDARD:
                if (points >= LoyaltyTier.SILVER.getRequiredPoints() &&
                    completedBookings >= LoyaltyTier.SILVER.getRequiredBookings()) {
                    return LoyaltyTier.SILVER;
                }
                break;

            case SILVER:
                if (points >= LoyaltyTier.GOLD.getRequiredPoints() &&
                    completedBookings >= LoyaltyTier.GOLD.getRequiredBookings()) {
                    return LoyaltyTier.GOLD;
                }
                break;

            case GOLD:
                if (points >= LoyaltyTier.PLATINUM.getRequiredPoints() &&
                    completedBookings >= LoyaltyTier.PLATINUM.getRequiredBookings()) {
                    return LoyaltyTier.PLATINUM;
                }
                break;

            case PLATINUM:
                // Đã đạt hạng cao nhất
                return LoyaltyTier.PLATINUM;
        }

        return currentTier; // Không đủ điều kiện nâng hạng
    }

    /**
     * Lấy yêu cầu để đạt tier tiếp theo
     */
    private Map<String, Object> getNextTierRequirements(LoyaltyTier currentTier) {
        Map<String, Object> requirements = new HashMap<>();

        switch (currentTier) {
            case STANDARD:
                requirements.put("tier", LoyaltyTier.SILVER);
                requirements.put("points", LoyaltyTier.SILVER.getRequiredPoints());
                requirements.put("bookings", LoyaltyTier.SILVER.getRequiredBookings());
                break;

            case SILVER:
                requirements.put("tier", LoyaltyTier.GOLD);
                requirements.put("points", LoyaltyTier.GOLD.getRequiredPoints());
                requirements.put("bookings", LoyaltyTier.GOLD.getRequiredBookings());
                break;

            case GOLD:
                requirements.put("tier", LoyaltyTier.PLATINUM);
                requirements.put("points", LoyaltyTier.PLATINUM.getRequiredPoints());
                requirements.put("bookings", LoyaltyTier.PLATINUM.getRequiredBookings());
                break;

            case PLATINUM:
                // Đã đạt hạng cao nhất
                return null;
        }

        return requirements;
    }

    /**
     * Scheduled job to check tier upgrades for all users daily
     * Runs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void scheduledTierUpgradeCheck() {
        log.info("Starting scheduled tier upgrade check for all users...");

        try {
            List<User> users = userRepository.findAll();
            int upgradedCount = 0;

            for (User user : users) {
                try {
                    LoyaltyTier originalTier = user.getLoyaltyTier();
                    checkAndUpgradeTier(user.getId());

                    // Check if tier was upgraded
                    userRepository.findById(user.getId()).ifPresent(updatedUser -> {
                        if (!Objects.equals(originalTier, updatedUser.getLoyaltyTier())) {
                            log.info("Scheduled upgrade: User {} upgraded from {} to {}",
                                    user.getId(), originalTier, updatedUser.getLoyaltyTier());
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to check tier upgrade for user {}: {}", user.getId(), e.getMessage());
                }
            }

            log.info("Completed scheduled tier upgrade check. Processed {} users", users.size());
        } catch (Exception e) {
            log.error("Error during scheduled tier upgrade check: {}", e.getMessage(), e);
        }
    }
}