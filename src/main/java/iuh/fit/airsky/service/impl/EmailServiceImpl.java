package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final FreeMarkerConfigurer freeMarkerConfigurer;

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setText(body, true);
            helper.setTo(to);
            helper.setSubject(subject);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new IllegalStateException("Failed to send email");
        }
    }

    @Override
    @Async
    public void sendHtmlTemplateEmail(String to, String subject, String templateName, Object model) {
        try {
            Template template = freeMarkerConfigurer.getConfiguration().getTemplate(templateName);
            String htmlBody = FreeMarkerTemplateUtils.processTemplateIntoString(template, (Map<String, Object>) model);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setText(htmlBody, true);
            helper.setTo(to);
            helper.setSubject(subject);
            mailSender.send(message);

            log.info("Template email sent to: {} with template: {}", to, templateName);
        } catch (MessagingException | IOException | TemplateException e) {
            log.error("Failed to send template email to: {} with template: {}", to, templateName, e);
            throw new IllegalStateException("Failed to send template email");
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
                "reviewUrl", "http://localhost:3000/my-bookings/" + bookingId + "/review" // Frontend URL
            );

            sendHtmlTemplateEmail(to, "Please share your flight experience with AirsKy", "ReviewInvitation.html", model);
            log.info("Review invitation email sent to: {} for booking: {}", to, bookingId);
        } catch (Exception e) {
            log.error("Failed to send review invitation email to: {} for booking: {}", to, bookingId, e);
            throw new IllegalStateException("Failed to send review invitation email");
        }
    }
}