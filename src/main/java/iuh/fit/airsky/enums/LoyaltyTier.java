package iuh.fit.airsky.enums;

import lombok.Getter;

import java.math.BigDecimal;

public enum LoyaltyTier {
    STANDARD("Standard", 0, BigDecimal.ZERO, 0, false, false),
    SILVER("Silver", 500, BigDecimal.valueOf(0.02), 5, false, false),
    GOLD("Gold", 1500, BigDecimal.valueOf(0.05), 15, true, false),
    PLATINUM("Platinum", 3000, BigDecimal.valueOf(0.10), 30, true, true);

    @Getter
    private final String displayName;

    @Getter
    private final int requiredPoints;

    @Getter
    private final BigDecimal discountRate;

    @Getter
    private final int requiredBookings;

    @Getter
    private final boolean priorityBoarding;

    @Getter
    private final boolean loungeAccess;

    LoyaltyTier(String displayName, int requiredPoints, BigDecimal discountRate,
                int requiredBookings, boolean priorityBoarding, boolean loungeAccess) {
        this.displayName = displayName;
        this.requiredPoints = requiredPoints;
        this.discountRate = discountRate;
        this.requiredBookings = requiredBookings;
        this.priorityBoarding = priorityBoarding;
        this.loungeAccess = loungeAccess;
    }

    public static LoyaltyTier getNextTier(LoyaltyTier current) {
        switch (current) {
            case STANDARD: return SILVER;
            case SILVER: return GOLD;
            case GOLD: return PLATINUM;
            case PLATINUM: return PLATINUM;
            default: return STANDARD;
        }
    }
}