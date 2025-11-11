package iuh.fit.airsky.service;

import iuh.fit.airsky.enums.LoyaltyTier;
import java.util.Map;

public interface LoyaltyService {
    /**
     * Kiểm tra và tự động nâng hạng thành viên nếu đủ điều kiện
     */
    void checkAndUpgradeTier(Long userId);

    /**
     * Tính toán hạng tiếp theo mà user có thể đạt được rồi trả về hạng tiếp theo hoặc null nếu đã đạt hạng cao nhất
     */
    LoyaltyTier getNextTier(Long userId);

    /**
     * Lấy thống kê loyalty của user
     * Map chứa thông tin points, tier hiện tại, tier tiếp theo, progress
     */
    Map<String, Object> getLoyaltyStats(Long userId);
}