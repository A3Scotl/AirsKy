package iuh.fit.airsky.enums;

import java.math.BigDecimal;

public enum BaggagePackage {
    KG_15(15, new BigDecimal("200000")),
    KG_20(20, new BigDecimal("300000")),
    KG_25(25, new BigDecimal("400000")),
    KG_30(30, new BigDecimal("500000"));

    private final int weightKg;
    private final BigDecimal price;

    BaggagePackage(int weightKg, BigDecimal price) {
        this.weightKg = weightKg;
        this.price = price;
    }

    public int getWeightKg() {
        return weightKg;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
