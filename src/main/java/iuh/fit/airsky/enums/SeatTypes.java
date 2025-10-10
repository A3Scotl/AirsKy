package iuh.fit.airsky.enums;

public enum SeatTypes {
    STANDARD("Ghế tiêu chuẩn"),            // Ghế tiêu chuẩn
    EXTRA_LEGROOM("Ghế rộng chỗ để chân"),       // Ghế rộng chỗ để chân
    EXIT_ROW("Ghế hàng thoát hiểm"),            // Ghế hàng thoát hiểm
    FRONT_ROW("Ghế hàng đầu"),           // Ghế hàng đầu / Bulkhead
    ACCESSIBLE("Ghế dành cho người khuyết tật");           // Ghế dành cho người khuyết tật

    private final String displayName;

    SeatTypes(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}