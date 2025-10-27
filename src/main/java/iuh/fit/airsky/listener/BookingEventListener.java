package iuh.fit.airsky.listener;

import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.event.BookingCancelledEvent;
import iuh.fit.airsky.event.BookingConfirmedEvent;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.service.impl.EmailTemplateGenerator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingEventListener {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final EmailTemplateGenerator emailTemplateGenerator;
    private final BookingService bookingService;

    @Async // Chạy bất đồng bộ để không block luồng chính
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Bắt đầu một transaction mới, độc lập
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingConfirmedEvent(BookingConfirmedEvent event) {

        Booking booking = event.getBooking();
        log.info("Handling BookingConfirmedEvent for booking {} in an async thread.", booking.getBookingId());
        try {
            // Lấy BookingResponse DTO đã được populate đầy đủ thông tin
            BookingResponse bookingResponse = bookingService.findById(booking.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found in event listener"));

            // 1. Gửi email xác nhận
            if (bookingResponse.getContactEmail() != null && !bookingResponse.getContactEmail().isBlank()) {
                String subject = "Xác Nhận Đặt Vé Thành Công - Mã: " + bookingResponse.getBookingCode();
                String htmlBody = emailTemplateGenerator.generateEmailTemplate(bookingResponse);
                emailService.sendEmail(bookingResponse.getContactEmail(), subject, htmlBody);
                log.info("Confirmation email sent to {} for booking {}", bookingResponse.getContactEmail(),
                        bookingResponse.getBookingId());
            }

            if (booking.getUserId() != null) {
                String message = String.format("Đặt vé %s của bạn đã được xác nhận thành công!",
                        booking.getBookingCode());
                notificationService.createAndSendNotification(
                        booking.getUserId().getId(),
                        "BOOKING_CONFIRMED",
                        message,
                        booking.getBookingId(),
                        "Đặt vé thành công");

            }
            log.info("Sent BOOKING_CONFIRMED notification for booking {}", booking.getBookingId());
        } catch (Exception e) {
            log.error("Failed to send BOOKING_CONFIRMED notification for booking {}: {}", booking.getBookingId(),
                    e.getMessage(), e);
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

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        // Ưu tiên email liên hệ được cung cấp trong booking
        if (booking.getContactEmail() != null && !booking.getContactEmail().isBlank()) {
            contactEmail = booking.getContactEmail();
            contactName = booking.getContactName();
        } else if (booking.getUserId() != null) { // Fallback về user email
            contactEmail = booking.getUserId().getEmail();
            contactName = booking.getUserId().getFirstName();
        }

        // 1. Gửi email thông báo hủy
        try {
            if (contactEmail != null && !contactEmail.isBlank()) {
                String subject = "Thông báo: Đặt vé đã bị hủy - Mã: " + booking.getBookingCode();
                // Bạn có thể tạo một template email riêng cho việc hủy vé
                String body = String.format(
                        "<h3>Xin chào %s,</h3><p>Đặt vé <b>%s</b> của bạn đã bị hủy do: <b>%s</b>.</p><p>Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi.</p>",
                        contactName, booking.getBookingCode(), reason);
                emailService.sendEmail(contactEmail, subject, body);
                log.info("Cancellation email sent to {} for booking {}", contactEmail, booking.getBookingId());
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation email for booking {}: {}", booking.getBookingId(), e.getMessage(),
                    e);
        }

        // 2. Gửi thông báo WebSocket
        try {
            if (booking.getUserId() != null) {
                String message = String.format("Đặt vé %s của bạn đã bị hủy do: %s.", booking.getBookingCode(), reason);
                notificationService.createAndSendNotification(
                        booking.getUserId().getId(),
                        "BOOKING_CANCELLED",
                        message,
                        booking.getBookingId(),
                        "Đặt vé đã bị hủy");
            }
        } catch (Exception e) {
            log.error("Failed to send BOOKING_CANCELLED notification for booking {}: {}", booking.getBookingId(),
                    e.getMessage(), e);
        }
    }
}
