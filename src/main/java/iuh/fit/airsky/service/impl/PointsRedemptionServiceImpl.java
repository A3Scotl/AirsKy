package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.PointsRedemptionRequest;
import iuh.fit.airsky.dto.response.DealResponse;
import iuh.fit.airsky.exception.ResourceNotFoundException;
import iuh.fit.airsky.mapper.DealMapper;
import iuh.fit.airsky.model.Deal;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.DealRepository;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.service.PointsRedemptionService;
import iuh.fit.airsky.util.GenerateCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointsRedemptionServiceImpl implements PointsRedemptionService {

    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final DealMapper dealMapper;
    private final GenerateCodeUtil generateCodeUtil;

    // Tỷ lệ đổi điểm: 100 điểm = 10,000 VND
    private static final int POINTS_PER_10K_VND = 100;
    private static final BigDecimal VND_PER_100_POINTS = BigDecimal.valueOf(10000);
    private static final int MIN_POINTS_REDEMPTION = 500; // Tối thiểu 500 điểm
    private static final int MAX_DISCOUNT_PERCENTAGE = 50; // Tối đa 50% giá trị booking

    @Override
    @Transactional
    public DealResponse redeemPointsForDeal(PointsRedemptionRequest request) {
        log.info("Processing points redemption for user: {}, points: {}", 
                request.getUserId(), request.getPointsToRedeem());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate user has enough points
        Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
        if (currentPoints < request.getPointsToRedeem()) {
            throw new IllegalArgumentException("Không đủ điểm để đổi thưởng. Điểm hiện tại: " + currentPoints);
        }

        // Validate minimum points
        if (request.getPointsToRedeem() < MIN_POINTS_REDEMPTION) {
            throw new IllegalArgumentException("Số điểm tối thiểu để đổi thưởng là: " + MIN_POINTS_REDEMPTION);
        }

        // Calculate discount amount
        BigDecimal discountAmount = calculateDiscountFromPoints(request.getPointsToRedeem());
        
        // Deduct points from user
        user.setLoyaltyPoints(currentPoints - request.getPointsToRedeem());
        userRepository.save(user);

        // Create points redemption deal
        Deal deal = Deal.builder()
                .dealCode(generateCodeUtil.generateDealCode(dealRepository))
                .title("Voucher đổi từ " + request.getPointsToRedeem() + " điểm")
                .description("Voucher giảm giá " + discountAmount.intValue() + " VND đổi từ điểm loyalty")
                .discountPercentage(BigDecimal.ZERO) // Use fixed amount instead
                .fixedDiscountAmount(discountAmount)
                .minimumOrderAmount(BigDecimal.valueOf(50000)) // Min 50k order
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(90)) // 3 months validity
                .maxDiscountAmount(discountAmount)
                .totalUsageLimit(1)
                .usedCount(0)
                .usagePerUser(1)
                .isActive(true)
                .isPointsRedemption(true)
                .pointsRequired(request.getPointsToRedeem())
                .isGuestOnly(false)
                .isLoyaltyExclusive(true)
                .requiredLoyaltyTier(user.getLoyaltyTier())
                .build();

        Deal savedDeal = dealRepository.save(deal);

        log.info("Created points redemption deal {} for user {} with discount: {}", 
                savedDeal.getDealCode(), user.getId(), discountAmount);

        return dealMapper.toResponseDTO(savedDeal);
    }

    @Override
    public BigDecimal calculateDiscountFromPoints(Integer points) {
        if (points < MIN_POINTS_REDEMPTION) {
            return BigDecimal.ZERO;
        }
        
        // 100 điểm = 10,000 VND
        return VND_PER_100_POINTS.multiply(BigDecimal.valueOf(points))
                .divide(BigDecimal.valueOf(POINTS_PER_10K_VND));
    }

    @Override
    public List<DealResponse> getUserPointsRedemptionDeals(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find active points redemption deals for this user's tier
        List<Deal> deals = dealRepository.findActivePointsRedemptionDeals(
                user.getLoyaltyTier(), LocalDateTime.now());

        return deals.stream()
                .map(dealMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getPointsRedemptionRates() {
        Map<String, Object> rates = new HashMap<>();
        rates.put("pointsPer10kVnd", POINTS_PER_10K_VND);
        rates.put("vndPer100Points", VND_PER_100_POINTS);
        rates.put("minPointsRedemption", MIN_POINTS_REDEMPTION);
        rates.put("maxDiscountPercentage", MAX_DISCOUNT_PERCENTAGE);
        rates.put("dealValidityDays", 90);
        
        // Suggested redemption tiers
        Map<Integer, BigDecimal> suggestedTiers = new HashMap<>();
        suggestedTiers.put(500, calculateDiscountFromPoints(500));   // 50k VND
        suggestedTiers.put(1000, calculateDiscountFromPoints(1000)); // 100k VND
        suggestedTiers.put(2000, calculateDiscountFromPoints(2000)); // 200k VND
        suggestedTiers.put(5000, calculateDiscountFromPoints(5000)); // 500k VND
        
        rates.put("suggestedTiers", suggestedTiers);
        return rates;
    }

    @Override
    public boolean canRedeemPoints(Long userId, Integer pointsRequired) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Integer currentPoints = user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0;
        return currentPoints >= pointsRequired && pointsRequired >= MIN_POINTS_REDEMPTION;
    }
}