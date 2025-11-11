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
    SYSTEM_ANNOUNCEMENT("Thông báo hệ thống"),
    NEW_PUBLIC_BLOG("Bài viết mới"),
    DEAL_ACTIVATED("Deal mới được kích hoạt"),
    REVIEW_REQUEST_CREATED("Yêu cầu đánh giá chuyến bay"),
    REVIEW_SUBMITTED("Đánh giá mới được gửi"),
    REVIEW_APPROVED("Đánh giá đã được duyệt"),
    REVIEW_HIDDEN("Đánh giá đã bị ẩn");

    private final String defaultTitle;

    NotificationType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }
}