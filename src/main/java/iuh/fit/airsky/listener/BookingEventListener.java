package iuh.fit.airsky.listener;

import iuh.fit.airsky.dto.response.BookingResponse;
import iuh.fit.airsky.enums.BookingStatus;
import iuh.fit.airsky.enums.PaymentStatus;
import iuh.fit.airsky.event.BookingCancelledEvent;
import iuh.fit.airsky.event.BookingConfirmedEvent;
import iuh.fit.airsky.model.Booking;
import iuh.fit.airsky.model.Passenger;
import iuh.fit.airsky.model.Payment;
import iuh.fit.airsky.service.BookingService;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.NotificationService;
import iuh.fit.airsky.service.impl.EmailTemplateGenerator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

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
        log.info("Handling BookingCancelledEvent for bookingId: {} on {}", booking.getBookingId(), LocalDateTime.now());                                                                                                         // (05/11/2025)

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.debug("Booking {} already cancelled, skipping event", booking.getBookingId());
            return;
        }

        String contactEmail = null;
        String contactName = null;

        // Ưu tiên email liên hệ
        if (booking.getContactEmail() != null && !booking.getContactEmail().isBlank()) {
            contactEmail = booking.getContactEmail();
            contactName = booking.getContactName();
        } else if (booking.getUserId() != null) {
            contactEmail = booking.getUserId().getEmail();
            contactName = booking.getUserId().getFirstName() + " " + booking.getUserId().getLastName();
        }

        // Lấy bank info từ payment để hướng dẫn manual refund
        String bankInfo = "N/A";
        Payment payment = booking.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            String account = payment.getPayerAccountNumber() != null ? payment.getPayerAccountNumber() : "N/A";
            String bank = payment.getPayerBankName() != null ? payment.getPayerBankName() : "N/A";
            bankInfo = String.format(
                    "Số tài khoản: %s, Ngân hàng: %s. Vui lòng liên hệ hỗ trợ để xử lý hoàn tiền thủ công.", account,
                    bank);
        }

        // 1. Gửi email thông báo hủy (dùng template generator để chèn bankInfo)
        try {
            if (contactEmail != null && !contactEmail.isBlank()) {
                String subject = String.format("Thông Báo Hủy Vé - Mã Đặt Chỗ: %s (Ngày %s)",
                        booking.getBookingCode(),
                        LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                // Sử dụng template generator (tương tự confirmed event)
                String htmlBody = emailTemplateGenerator.generateCancellationTemplate(booking, reason, bankInfo);                                                                                          // này
                emailService.sendEmail(contactEmail, subject, htmlBody);
                log.info("Cancellation email sent to {} for booking {} with bank info: {}", contactEmail,
                        booking.getBookingId(), bankInfo);
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation email for booking {}: {}", booking.getBookingId(), e.getMessage(),
                    e);
        }

        // 2. Gửi thông báo WebSocket (thêm bank info ngắn gọn nếu cần)
        try {
            if (booking.getUserId() != null) {
                String message = String.format("Đặt vé %s bị hủy do: %s. Hoàn tiền qua %s.",
                        booking.getBookingCode(), reason,
                        bankInfo.length() > 50 ? "tài khoản ngân hàng đã lưu" : bankInfo);
                notificationService.createAndSendNotification(
                        booking.getUserId().getId(),
                        "BOOKING_CANCELLED",
                        message,
                        booking.getBookingId(),
                        "Vé Đã Bị Hủy");
                log.info("BOOKING_CANCELLED notification sent for booking {}", booking.getBookingId());
            }
        } catch (Exception e) {
            log.error("Failed to send BOOKING_CANCELLED notification for booking {}: {}", booking.getBookingId(),
                    e.getMessage(), e);
        }
    }
}
