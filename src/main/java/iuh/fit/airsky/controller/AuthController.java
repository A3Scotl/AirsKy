package iuh.fit.airsky.controller;

import iuh.fit.airsky.dto.request.auth.*;
import iuh.fit.airsky.dto.response.ApiResponse;
import iuh.fit.airsky.dto.response.AuthResponse;
import iuh.fit.airsky.dto.response.UserResponse;
import iuh.fit.airsky.exception.AuthException;
import iuh.fit.airsky.exception.EmailNotVerifiedException;
import iuh.fit.airsky.exception.GoogleAuthException;
import iuh.fit.airsky.exception.InvalidCredentialsException;
import iuh.fit.airsky.mapper.UserMapper;
import iuh.fit.airsky.service.AuthService;
import iuh.fit.airsky.service.GoogleAuthService;
import iuh.fit.airsky.util.ApiResponseUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ApiResponseUtil.buildResponse(true, "User registered successfully. Verification code sent.", response, "/api/v1/auth/register");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed", ex.getMessage(), "/api/v1/auth/register");
        }
    }

    @PostMapping("/verify-registration")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistration(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            AuthResponse response = authService.verifyRegistration(request);
            return ApiResponseUtil.buildResponse(true, "Email verified successfully", response, "/api/v1/auth/verify-registration");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Verification failed", ex.getMessage(), "/api/v1/auth/verify-registration");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ApiResponseUtil.buildResponse(true, "Login successful", response, "/api/v1/auth/login");
        } catch (InvalidCredentialsException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS", "/api/v1/auth/login");
        } catch (EmailNotVerifiedException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "EMAIL_NOT_VERIFIED", "/api/v1/auth/login");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Login failed", ex.getMessage(), "/api/v1/auth/login");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<AuthResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            return ApiResponseUtil.buildResponse(true, "Verification code sent to email", null, "/api/v1/auth/forgot-password");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Forgot password failed", ex.getMessage(), "/api/v1/auth/forgot-password");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<AuthResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ApiResponseUtil.buildResponse(true, "Password reset successful", null, "/api/v1/auth/reset-password");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Reset password failed", ex.getMessage(), "/api/v1/auth/reset-password");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<AuthResponse>> resendVerificationCode(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.resendVerificationCode(request);
            return ApiResponseUtil.buildResponse(true, "Verification code resent to email", null, "/api/v1/auth/resend-verification");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Resend verification failed", ex.getMessage(), "/api/v1/auth/resend-verification");
        }
    }


    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER')")
    public ResponseEntity<ApiResponse<AuthResponse>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePassword(request);
            return ApiResponseUtil.buildResponse(true, "Change password successful", null, "/api/v1/auth/change-password");
        } catch (InvalidCredentialsException ex) {
            return ApiResponseUtil.buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS", "/api/v1/auth/change-password");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Change password failed", ex.getMessage(), "/api/v1/auth/change-password");
        }
    }

    @GetMapping("/profile/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            UserResponse user = authService.getUserByEmail(email);

            return ApiResponseUtil.buildResponse(
                    true,
                    "Get user profile successful",
                    user,
                    "/api/v1/auth/profile/me"
            );
        } catch (AuthException ex) {
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    ex.getMessage(),
                    "INVALID_CREDENTIALS",
                    "/api/v1/auth/profile/me"
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Get user profile failed",
                    ex.getMessage(),
                    "/api/v1/auth/profile/me"
            );
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        try {
            clearAuthCookie(response);
            return ApiResponseUtil.buildResponse(true, "Logged out successfully", null, "/api/v1/auth/logout");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Logout failed", ex.getMessage(), "/api/v1/auth/logout");
        }
    }

    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse response = googleAuthService.loginWithGoogle(request.getIdToken());
            return ApiResponseUtil.buildResponse(
                    true,
                    "Google login successful",
                    response,
                    "/api/v1/auth/google-login"
            );
        } catch (GoogleAuthException ex) {
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    ex.getMessage(),
                    "GOOGLE_AUTH_FAILED",
                    "/api/v1/auth/google-login"
            );
        } catch (Exception ex) {
            return ApiResponseUtil.buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Google login failed",
                    ex.getMessage(),
                    "/api/v1/auth/google-login"
            );
        }
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuthResponse>> adminRegister(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ApiResponseUtil.buildResponse(true, "Admin created user successfully.", response, "/api/v1/auth/admin/register");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ApiResponseUtil.buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Admin registration failed", ex.getMessage(), "/api/v1/auth/admin/register");
        }
    }

}
