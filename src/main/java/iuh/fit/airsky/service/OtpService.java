package iuh.fit.airsky.service;

import iuh.fit.airsky.model.User;
import iuh.fit.airsky.model.VerificationToken;

public interface OtpService {
    void createAndSendOtp(String email);
    void resendOtp(String email);
    VerificationToken validateOtp(String email, String otpCode);
}
