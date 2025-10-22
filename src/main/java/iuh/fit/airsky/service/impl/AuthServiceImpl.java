package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.dto.request.auth.*;
import iuh.fit.airsky.dto.response.AuthResponse;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.enums.AuthProvider;
import iuh.fit.airsky.enums.LoyaltyTier;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
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
            throw new AuthException("Email đã tồn tại");
        }
        validateEmailFormat(request.getEmail());
        validateEmailExists(request.getEmail());
        validatePasswordStrength(request.getPassword());
        validatePhoneNumber(request.getPhone());

        // Dùng UserMapper để map
        User user = userMapper.toEntity(request.toUserRequest());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Determine role: only admin can set role other than CUSTOMER
        Role roleToSet = Role.CUSTOMER;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        if (authentication != null && authentication.isAuthenticated() && authentication.getAuthorities() != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                    isAdmin = true;
                    break;
                }
            }
        }
        if (isAdmin && request.getRole() != null) {
            roleToSet = request.getRole();
        }
        user.setRole(roleToSet);
        user.setVerified(false);
        user.setActive(true);
        user.setDeleted(false);
        user.setAuthProvider(AuthProvider.LOCAL);

        user.setLoyaltyTier(LoyaltyTier.STANDARD);
        user.setLoyaltyPoints(0);

        userRepository.save(user);
        otpService.createAndSendOtp(user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Tài khoản hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Tài khoản hoặc mật khẩu không đúng");
        }
        if (!user.isVerified()) {
            throw new AuthException("Email chưa được xác thực");
        }
        if (!user.isActive()) {
            throw new AuthException("Tài khoản đã bị khóa");
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
    }

    @Override
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Email không tồn tại"));

        if (!user.isActive()) {
            throw new AuthException("Tài khoản đã bị khóa");
        }

        otpService.createAndSendOtp(user.getEmail());
        return null;
    }

    @Override
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng"));

        otpService.validateOtp(request.getEmail(), request.getOtpCode());

        user.setVerified(true);
        userRepository.save(user);

        verificationTokenRepository.deleteByEmail(request.getEmail());

        return null;
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng"));
        return userMapper.toResponseDTO(user);
    }

    @Override
    public AuthResponse resendVerificationCode(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Email không tồn tại"));

        if (!user.isActive()) {
            throw new AuthException("Tài khoản đã bị khóa");
        }

        otpService.resendOtp(user.getEmail());
        return null;
    }

    @Override
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng"));

        if (!user.isActive()) {
            throw new AuthException("Tài khoản đã bị khóa");
        }

        otpService.validateOtp(request.getEmail(), request.getOtpCode());
        validatePasswordStrength(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return null;
    }

    @Override
    public AuthResponse changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng"));

        if (!user.isActive()) {
            throw new AuthException("Tài khoản đã bị khóa");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AuthException("Mật khẩu cũ không đúng");
        }

        validatePasswordStrength(request.getNewPassword());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return null;
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new AuthResponse(accessToken, refreshToken);
    }

    private void validatePasswordStrength(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new AuthException("Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
        }
    }

    private void validatePhoneNumber(String phone) {
        // Chỉ cho ph��p số điện thoại Việt Nam, 10 số, bắt đầu bằng 0
        if (!phone.matches("^0[0-9]{9}$")) {
            throw new AuthException("Số điện thoại phải bắt đầu bằng 0 và đủ 10 số");
        }
    }

    private void validateEmailExists(String email) {
        try {
            javax.naming.directory.InitialDirContext ctx = new javax.naming.directory.InitialDirContext();
            javax.naming.directory.Attributes attrs = ctx.getAttributes("dns:/" + email.substring(email.indexOf("@") + 1), new String[] {"MX"});
            if (attrs == null || attrs.size() == 0) {
                throw new AuthException("Email không tồn tại hoặc không thể nhận thư");
            }
        } catch (Exception e) {
            throw new AuthException("Email không tồn tại hoặc không thể nhận thư");
        }
    }

    private void validateEmailFormat(String email) {
        // Chỉ cho phép email có domain kết thúc bằng .com, .net, .org, .edu, .info, .vn, ... (ít nhất 3 ký tự)
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{3,}$")) {
            throw new AuthException("Email không đúng định dạng. Vui lòng nhập email có dạng example@gmail.com");
        }
    }
}
