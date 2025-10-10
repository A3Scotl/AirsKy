package iuh.fit.airsky.enums;

import java.math.BigDecimal;

public enum SeatTypePrice {
    STANDARD(BigDecimal.ZERO, "Ghế tiêu chuẩn"),
    EXTRA_LEGROOM(BigDecimal.valueOf(50000), "Ghế thêm không gian chân"),
    EXIT_ROW(BigDecimal.valueOf(100000), "Ghế lối thoát hiểm"),
    FRONT_ROW(BigDecimal.valueOf(75000), "Ghế hàng đầu"),
    ACCESSIBLE(BigDecimal.valueOf(25000), "Ghế cho người khuyết tật");

    private final BigDecimal additionalPrice;
    private final String description;

    SeatTypePrice(BigDecimal additionalPrice, String description) {
        this.additionalPrice = additionalPrice;
        this.description = description;
    }

    public BigDecimal getAdditionalPrice() {
        return additionalPrice;
    }

    public String getDescription() {
        return description;
    }

    public static SeatTypePrice fromSeatType(SeatTypes seatType) {
        return switch (seatType) {
            case STANDARD -> STANDARD;
            case EXTRA_LEGROOM -> EXTRA_LEGROOM;
            case EXIT_ROW -> EXIT_ROW;
            case FRONT_ROW -> FRONT_ROW;
            case ACCESSIBLE -> ACCESSIBLE;
        };
    }
}