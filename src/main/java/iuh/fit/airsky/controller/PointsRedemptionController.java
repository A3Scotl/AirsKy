package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.PointsRedemptionRequest;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.service.PointsRedemptionService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/points-redemption")
@RequiredArgsConstructor
@Slf4j
public class PointsRedemptionController {

    private final PointsRedemptionService pointsRedemptionService;

    @PostMapping("/redeem")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<DealResponse>> redeemPointsForDeal(
            @Valid @RequestBody PointsRedemptionRequest request) {
        log.info("Redeeming points for user: {}", request.getUserId());
        try {
            DealResponse deal = pointsRedemptionService.redeemPointsForDeal(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Đổi điểm thành công", deal, null, null, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null, e.getMessage(), null, null));
        } catch (Exception e) {
            log.error("Error redeeming points", e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Có lỗi xảy ra khi đổi điểm", null, "Internal server error", null, null));
        }
    }

    @GetMapping("/calculate-discount")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateDiscountFromPoints(
            @RequestParam Integer points) {
        BigDecimal discount = pointsRedemptionService.calculateDiscountFromPoints(points);
        return ResponseEntity.ok(new ApiResponse<>(true, "Giá trị giảm giá cho " + points + " điểm", discount, null, null, null));
    }

    @GetMapping("/calculate-discount-by-membership")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateDiscountFromPointsByMembership(
            @RequestParam String membershipCode,
            @RequestParam Integer points) {
        try {
            BigDecimal discount = pointsRedemptionService.calculateDiscountFromPointsByMembershipCode(membershipCode, points);
            return ResponseEntity.ok(new ApiResponse<>(true, "Giá trị giảm giá cho " + points + " điểm của mã hội viên " + membershipCode, discount, null, null, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null, e.getMessage(), null, null));
        } catch (Exception e) {
            log.error("Error calculating discount by membership code", e);
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Có lỗi xảy ra khi tính giảm giá", null, "Internal server error", null, null));
        }
    }

    @GetMapping("/user/{userId}/deals")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<List<DealResponse>>> getUserPointsRedemptionDeals(
            @PathVariable Long userId) {
        List<DealResponse> deals = pointsRedemptionService.getUserPointsRedemptionDeals(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Danh sách deal đổi điểm cho user", deals, null, null, null));
    }

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPointsRedemptionRates() {
        Map<String, Object> rates = pointsRedemptionService.getPointsRedemptionRates();
        return ResponseEntity.ok(new ApiResponse<>(true, "Thông tin tỷ lệ đổi điểm", rates, null, null, null));
    }

    @GetMapping("/user/{userId}/can-redeem")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<Boolean>> canRedeemPoints(
            @PathVariable Long userId,
            @RequestParam Integer pointsRequired) {
        boolean canRedeem = pointsRedemptionService.canRedeemPoints(userId, pointsRequired);
        return ResponseEntity.ok(new ApiResponse<>(true, canRedeem ? "Có thể đổi điểm" : "Không đủ điểm để đổi", canRedeem, null, null, null));
    }

    @GetMapping("/can-redeem-by-membership")
    public ResponseEntity<ApiResponse<Boolean>> canRedeemPointsByMembership(
            @RequestParam String membershipCode,
            @RequestParam Integer pointsRequired) {
        boolean canRedeem = pointsRedemptionService.canRedeemPointsByMembershipCode(membershipCode, pointsRequired);
        String message = canRedeem ? "Có thể đổi điểm với mã hội viên " + membershipCode : "Không đủ điểm để đổi với mã hội viên " + membershipCode;
        return ResponseEntity.ok(new ApiResponse<>(true, message, canRedeem, null, null, null));
    }
}