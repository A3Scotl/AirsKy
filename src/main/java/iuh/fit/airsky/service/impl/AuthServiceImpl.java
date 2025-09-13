package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.auth.*;
import iuh.fit.airsky.dto.response.AuthResponse;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.enums.Role;
import iuh.fit.airsky.exception.AuthException;
import iuh.fit.airsky.mapper.UserMapper;
import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.repository.VerificationTokenRepository;
import iuh.fit.airsky.service.AuthService;
import iuh.fit.airsky.service.JwtService;
import iuh.fit.airsky.service.OtpService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @PostConstruct
    public void initAdminAccount() {
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User admin = User.builder()
                    .email("admin@gmail.com")
                    .firstName("Admin")
                    .lastName("User")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ADMIN)
                    .isVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("✅ Default admin account created successfully");
        }
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new AuthException("Email already exists");
        }

        validatePasswordStrength(request.getPassword());

        // Dùng UserMapper để map
        User user = userMapper.toEntity(request.toUserRequest());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setVerified(false);
        user.setActive(true);
        user.setDeleted(false);

        userRepository.save(user);
        otpService.createAndSendOtp(user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid credentials");
        }
        if (!user.isVerified()) {
            throw new AuthException("Email not verified");
        }
        if (!user.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        // Update lastLogin
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }    @Override
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Email not found"));

        if (!user.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        otpService.createAndSendOtp(user.getEmail());
        return new AuthResponse("OTP has been sent to your email", null);
    }

    @Override
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("User not found"));

        otpService.validateOtp(request.getEmail(), request.getOtpCode());

        user.setVerified(true);
        userRepository.save(user);

        verificationTokenRepository.deleteByEmail(request.getEmail());

        return new AuthResponse("Email verified successfully", null);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AuthException("User not found"));
        return userMapper.toResponseDTO(user);
    }

    @Override
    public AuthResponse resendVerificationCode(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Email not found"));

        if (!user.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        otpService.resendOtp(user.getEmail());
        return new AuthResponse("OTP has been resent to your email", null);
    }

    @Override
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!user.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        otpService.validateOtp(request.getEmail(), request.getOtpCode());
        validatePasswordStrength(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new AuthResponse("Password reset successfully", null);
    }

    @Override
    public AuthResponse changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("User not found"));

        if (!user.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AuthException("Incorrect old password");
        }

        validatePasswordStrength(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new AuthResponse("Password changed successfully", null);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new AuthResponse(accessToken, refreshToken);
    }

    private void validatePasswordStrength(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new AuthException("Password must be at least 8 characters, including uppercase, lowercase, digit, and special character");
        }
    }
}
