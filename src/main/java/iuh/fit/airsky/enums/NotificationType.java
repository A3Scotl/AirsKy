package iuh.fit.airsky.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    BOOKING_CONFIRMED("Đặt vé thành công"),
    BOOKING_CANCELLED("Đặt vé đã bị hủy"),
    PAYMENT_SUCCESS("Thanh toán thành công"),
    PAYMENT_FAILED("Thanh toán thất bại"),
    FLIGHT_DELAYED("Chuyến bay bị trễ"),
    GATE_CHANGE("Thay đổi cổng ra máy bay"),
    CHECKIN_OPEN("Check-in đã mở"),
    CHECKIN_REMINDER("Nhắc nhở check-in"),
    CHECKIN_SUCCESSFUL("Check-in thành công"),
    LOYALTY_POINTS_AWARDED("Tích điểm thành công"),
    LOYALTY_TIER_UPGRADE("Thăng hạng thành viên"),
    SYSTEM_ANNOUNCEMENT("Thông báo hệ thống");

    private final String defaultTitle;

    NotificationType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }
}