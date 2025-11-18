package iuh.fit.airsky.service.impl;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import iuh.fit.airsky.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

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
            Map<String, Object> model = Map.of(
                "userName", userName,
                "bookingId", bookingId,
                "flightNumber", flightNumber,
                "reviewUrl", "https://www.airsky.online/my-bookings/" + bookingId + "/review" // Frontend URL
            );

            sendHtmlTemplateEmail(to, "Please share your flight experience with AirsKy", "ReviewInvitation.html", model);
            log.info("Review invitation email sent to: {} for booking: {}", to, bookingId);
        } catch (Exception e) {
            log.error("Failed to send review invitation email to: {} for booking: {}", to, bookingId, e);
            throw new IllegalStateException("Failed to send review invitation email: " + e.getMessage());
        }
    }
}