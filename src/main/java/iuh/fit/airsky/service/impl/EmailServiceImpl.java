package iuh.fit.airsky.service.impl;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import iuh.fit.airsky.model.Review;
import iuh.fit.airsky.repository.ReviewRepository;
import iuh.fit.airsky.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.beans.factory.annotation.Value;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final Resend resend;
    private final FreeMarkerConfigurer freeMarkerConfigurer;
    private final ReviewRepository reviewRepository;
    
    @Value("${app.base-url}")
    private String baseUrl;

    private static final String FROM_EMAIL = "AirsKy <noreply@airsky.online>";

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(FROM_EMAIL)
                    .to(to)
                    .subject(subject)
                    .html(body)  
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("Email sent to: {} with ID: {}", to, response.getId());
        } catch (ResendException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new IllegalStateException("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendHtmlTemplateEmail(String to, String subject, String templateName, Object model) {
        try {
            Template template = freeMarkerConfigurer.getConfiguration().getTemplate(templateName);
            String htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(template, (Map<String, Object>) model);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(FROM_EMAIL)
                    .to(to)
                    .subject(subject)
                    .html(htmlBody)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("Template email sent to: {} with template: {} and ID: {}", to, templateName, response.getId());
        } catch (ResendException e) {
            log.error("Failed to send template email to: {} with template: {}", to, templateName, e);
            throw new IllegalStateException("Failed to send template email: " + e.getMessage());
        } catch (IOException | TemplateException ex) {
            log.error("Failed to process template: {}", templateName, ex);
            throw new IllegalStateException("Failed to process template email");
        }
    }

    @Override
    @Async
    public void sendReviewInvitationEmail(String to, String userName, Long bookingId, String flightNumber) {
        try {
            // Tạo booking code từ bookingId (format: AK + bookingId với padding)
            String bookingCode = String.format("AK%06d", bookingId);
            
            Map<String, Object> model = Map.of(
                "passengerName", userName,
                "bookingId", bookingId,
                "bookingCode", bookingCode, 
                "flightNumber", flightNumber,
                "flightDate", java.time.LocalDate.now().toString(), // Có thể lấy từ database sau
                "departureAirport", "HCM", // Cần lấy từ database
                "arrivalAirport", "HAN", // Cần lấy từ database  
                "reviewUrl", "https://www.airsky.online/my-bookings/" + bookingId + "/review"
            );

            sendHtmlTemplateEmail(to, "Mời đánh giá chuyến bay - AirsKy Airlines", "ReviewInvitation.html", model);
            log.info("Review invitation email sent to: {} for booking: {}", to, bookingId);
        } catch (Exception e) {
            log.error("Failed to send review invitation email to: {} for booking: {}", to, bookingId, e);
            throw new IllegalStateException("Failed to send review invitation email: " + e.getMessage());
        }
    }

    @Override
    public void sendReviewInvitationEmailWithFullInfo(String to, String userName, iuh.fit.airsky.model.Review review) {
        // Fetch all data BEFORE calling async to avoid session closed errors
        try {
            iuh.fit.airsky.model.Booking booking = review.getBooking();
            iuh.fit.airsky.model.Flight flight = review.getFlight();
            iuh.fit.airsky.model.User user = review.getUser();
            
            // Extract all needed data while session is still open
            Long bookingId = booking.getBookingId();
            Long userId = user.getId();
            Long flightId = flight.getFlightId();
            String bookingCode = booking.getBookingCode() != null ? booking.getBookingCode() : "AK" + booking.getBookingId();
            String flightNumber = flight.getFlightNumber() != null ? flight.getFlightNumber() : "VN" + flight.getFlightId();
            String flightDate = flight.getDepartureTime() != null ? 
                flight.getDepartureTime().toLocalDate().toString() : "2025-11-22";
            
            // Use actual airport data or meaningful fallbacks instead of generic codes
            String departureAirport = "N/A";
            String arrivalAirport = "N/A";
            
            // Try to get real airport info if available
            try {
                if (booking.getFlightSegments() != null && !booking.getFlightSegments().isEmpty()) {
                    var segment = booking.getFlightSegments().get(0);
                    if (segment.getDepartureAirport() != null) {
                        departureAirport = segment.getDepartureAirport().getAirportCode();
                    }
                    if (segment.getArrivalAirport() != null) {
                        arrivalAirport = segment.getArrivalAirport().getAirportCode();
                    }
                }
            } catch (Exception e) {
                log.warn("Could not get airport info, using N/A: {}", e.getMessage());
            }
            
            // Call async method with extracted data
            sendReviewEmailAsync(to, userName, bookingId, userId, flightId, bookingCode, 
                               flightNumber, flightDate, departureAirport, arrivalAirport);
                               
        } catch (Exception e) {
            log.error("Failed to prepare review invitation email", e);
            throw new IllegalStateException("Failed to send review invitation email: " + e.getMessage());
        }
    }

    @Async
    private void sendReviewEmailAsync(String to, String userName, Long bookingId, Long userId, Long flightId,
                                    String bookingCode, String flightNumber, String flightDate,
                                    String departureAirport, String arrivalAirport) {
        try {
            Map<String, Object> model = Map.of(
                "passengerName", userName,
                "bookingId", bookingId,
                "userId", userId,
                "flightId", flightId,
                "bookingCode", bookingCode,
                "flightNumber", flightNumber,
                "flightDate", flightDate,
                "departureAirport", departureAirport,
                "arrivalAirport", arrivalAirport,
                "baseUrl", baseUrl
            );

            sendHtmlTemplateEmail(to, "Mời đánh giá chuyến bay - AirsKy Airlines", "ReviewInvitation.html", model);
            log.info("Review invitation email sent to: {} for booking: {}", to, bookingId);
        } catch (Exception e) {
            log.error("Failed to send async review email to: {}", to, e);
        }
    }

    @Override
    public void sendReviewInvitationEmailWithFullInfo(Long bookingId, Long userId, String email) {
        // Implementation delegate to existing method
        // This is a convenience overload
        try {
            // Get review by booking and user  
            Review review = reviewRepository.findLatestByBookingIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new RuntimeException("Review not found for booking " + bookingId + " and user " + userId));
                
            String userName = review.getUser().getFirstName() + " " + review.getUser().getLastName();
            sendReviewInvitationEmailWithFullInfo(email, userName, review);
        } catch (Exception e) {
            log.error("Failed to send review email for booking {} and user {}", bookingId, userId, e);
            throw new RuntimeException("Failed to send review email: " + e.getMessage());
        }
    }
}