package iuh.fit.airsky.listener;

import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.event.BookingCancelledEvent;
import iuh.fit.airsky.event.BookingConfirmedEvent;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.service.impl.EmailTemplateGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingEventListener {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final EmailTemplateGenerator emailTemplateGenerator;

    @Async // Chạy bất đồng bộ để không block luồng chính
    @EventListener
    public void handleBookingConfirmedEvent(BookingConfirmedEvent event) {
        Booking booking = event.getBooking();
        log.info("Handling BookingConfirmedEvent for bookingId: {}", booking.getBookingId());

        String contactEmail = null;
        String contactName = null;

        if (booking.getUserId() != null) {
            contactEmail = booking.getUserId().getEmail();
            contactName = booking.getUserId().getFirstName();
        } else if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
            // Lấy thông tin từ hành khách đầu tiên cho guest booking
            Passenger firstPassenger = booking.getPassengers().get(0);
            contactEmail = firstPassenger.getEmail();
            contactName = firstPassenger.getFirstName();
        }

        // 1. Gửi email xác nhận
        try {
            if (contactEmail != null && !contactEmail.isBlank()) {
                String subject = "Xác Nhận Đặt Vé Thành Công - Mã: " + booking.getBookingCode();
                String htmlBody = emailTemplateGenerator.generateEmailTemplate(booking);
                emailService.sendEmail(contactEmail, subject, htmlBody);
                log.info("Confirmation email sent to {} for booking {}", contactEmail, booking.getBookingId());
            }
        } catch (Exception e) {
            log.error("Failed to send confirmation email for booking {}: {}", booking.getBookingId(), e.getMessage(), e);
        }

        // 2. Gửi thông báo WebSocket
        try {
            if (booking.getUserId() != null) {
                String message = String.format("Đặt vé %s của bạn đã được xác nhận thành công!", booking.getBookingCode());
                notificationService.sendNotificationToUserWithRelatedId(
                    booking.getUserId().getId(),
                    "BOOKING_CONFIRMED",
                    message,
                    booking.getBookingId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send BOOKING_CONFIRMED notification for booking {}: {}", booking.getBookingId(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void handleBookingCancelledEvent(BookingCancelledEvent event) {
        Booking booking = event.getBooking();
        String reason = event.getReason();
        log.info("Handling BookingCancelledEvent for bookingId: {}", booking.getBookingId());

        String contactEmail = null;
        String contactName = null;

        if(booking.getStatus() == BookingStatus.CANCELLED){
            return;
        }
        if (booking.getUserId() != null) {
            contactEmail = booking.getUserId().getEmail();
            contactName = booking.getUserId().getFirstName();
        } else if (booking.getPassengers() != null && !booking.getPassengers().isEmpty()) {
            Passenger firstPassenger = booking.getPassengers().get(0);
            contactEmail = firstPassenger.getEmail();
            contactName = firstPassenger.getFirstName();
        }

        // 1. Gửi email thông báo hủy
        try {
            if (contactEmail != null && !contactEmail.isBlank()) {
                String subject = "Thông báo: Đặt vé đã bị hủy - Mã: " + booking.getBookingCode();
                // Bạn có thể tạo một template email riêng cho việc hủy vé
                String body = String.format(
                    "<h3>Xin chào %s,</h3><p>Đặt vé <b>%s</b> của bạn đã bị hủy do: <b>%s</b>.</p><p>Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi.</p>",
                    contactName, booking.getBookingCode(), reason
                );
                emailService.sendEmail(contactEmail, subject, body);
                log.info("Cancellation email sent to {} for booking {}", contactEmail, booking.getBookingId());
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation email for booking {}: {}", booking.getBookingId(), e.getMessage(), e);
        }

        // 2. Gửi thông báo WebSocket
        try {
            if (booking.getUserId() != null) {
                String message = String.format("Đặt vé %s của bạn đã bị hủy do: %s.", booking.getBookingCode(), reason);
                notificationService.sendNotificationToUserWithRelatedId(
                    booking.getUserId().getId(),
                    "BOOKING_CANCELLED",
                    message,
                    booking.getBookingId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send BOOKING_CANCELLED notification for booking {}: {}", booking.getBookingId(), e.getMessage(), e);
        }
    }
}
