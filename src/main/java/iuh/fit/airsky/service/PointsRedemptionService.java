package iuh.fit.airsky.service;

import iuh.fit.airsky.dto.request.PointsRedemptionRequest;
import iuh.fit.airsky.dto.response.DealResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PointsRedemptionService {
    
    /**
     * Đổi điểm thành deal giảm giá
     */
    DealResponse redeemPointsForDeal(PointsRedemptionRequest request);
    
    /**
     * Tính toán số tiền giảm giá từ số điểm
     */
    BigDecimal calculateDiscountFromPoints(Integer points);
    
    /**
     * Tính toán số tiền giảm giá từ số điểm cho membership code (không cần đăng nhập)
     */
    BigDecimal calculateDiscountFromPointsByMembershipCode(String membershipCode, Integer points);
    
    /**
     * Lấy danh sách deal đổi điểm của user
     */
    List<DealResponse> getUserPointsRedemptionDeals(Long userId);
    
    /**
     * Lấy tỷ lệ đổi điểm hiện tại
     */
    Map<String, Object> getPointsRedemptionRates();
    
    /**
     * Kiểm tra user có đủ điểm để đổi không
     */
    boolean canRedeemPoints(Long userId, Integer pointsRequired);
    
    /**
     * Kiểm tra membership code có đủ điểm để đổi không (không cần đăng nhập)
     */
    boolean canRedeemPointsByMembershipCode(String membershipCode, Integer pointsRequired);
}