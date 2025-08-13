package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.exception.AuthException;
import iuh.fit.airsky.model.VerificationToken;
import iuh.fit.airsky.repository.VerificationTokenRepository;
import iuh.fit.airsky.service.EmailService;
import iuh.fit.airsky.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpServiceImpl implements OtpService {

    private static final int MAX_RESEND_COUNT = 3;
    private static final long RESEND_TIME_WINDOW_MINUTES = 60;
    private static final long MIN_INTERVAL_SECONDS = 60;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    @Value("${app.verification.expiration-minutes:5}")
    private int verificationExpirationMinutes;

    @Override
    public void createAndSendOtp(String email) {
        String verificationCode = generateVerificationCode(email);
        LocalDateTime now = LocalDateTime.now();

        verificationTokenRepository.findByEmail(email)
                .ifPresent(verificationTokenRepository::delete);

        VerificationToken token = VerificationToken.builder()
                .email(email)
                .code(verificationCode)
                .timestamp(now)
                .resendCount(0)
                .lastResendTime(now)
                .build();

        verificationTokenRepository.save(token);
        sendOtpEmail(email, verificationCode);
    }

    @Override
    public void resendOtp(String email) {
        VerificationToken token = verificationTokenRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("No password reset request found"));

        validateResendLimit(token);

        String verificationCode = generateVerificationCode(email);
        LocalDateTime now = LocalDateTime.now();

        token.setCode(verificationCode);
        token.setTimestamp(now);
        token.setResendCount(token.getResendCount() + 1);
        token.setLastResendTime(now);

        verificationTokenRepository.save(token);
        sendOtpEmail(email, verificationCode);
    }

    @Override
    public VerificationToken validateOtp(String email, String otpCode) {
        VerificationToken token = verificationTokenRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("No verification token found"));

        if (isOtpExpired(token)) {
            throw new AuthException("Verification code expired");
        }

        if (!token.getCode().equals(otpCode)) {
            throw new AuthException("Invalid verification code");
        }

        return token;
    }

    private String generateVerificationCode(String email) {
        long seed = System.nanoTime()
                ^ email.hashCode()
                ^ LocalTime.now().toNanoOfDay();
        Random random = new Random(seed);

        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }



    private void validateResendLimit(VerificationToken token) {
        LocalDateTime now = LocalDateTime.now();
        // Check khoảng cách tối thiểu giữa 2 lần gửi
        if (token.getLastResendTime() != null &&
                java.time.Duration.between(token.getLastResendTime(), now).getSeconds() < MIN_INTERVAL_SECONDS) {
            throw new AuthException("Please wait at least " + MIN_INTERVAL_SECONDS + " seconds before requesting another OTP.");
        }

        // Check giới hạn số lần gửi trong thời gian quy định
        if (token.getResendCount() >= MAX_RESEND_COUNT &&
                now.isBefore(token.getLastResendTime().plusMinutes(RESEND_TIME_WINDOW_MINUTES))) {
            throw new AuthException("Resend limit reached. Please try again later.");
        }
    }

    private boolean isOtpExpired(VerificationToken token) {
        return LocalDateTime.now().isAfter(
                token.getTimestamp().plusMinutes(verificationExpirationMinutes)
        );
    }

    private void sendOtpEmail(String email, String code) {
        String subject = "Your Verification Code";
        String body = String.format("Your verification code is: %s. It will expire in %d minutes.",
                code, verificationExpirationMinutes);

        emailService.sendVerificationEmail(email, subject, body);
    }
}