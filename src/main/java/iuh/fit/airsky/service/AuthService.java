/*
 * @ (#) AuthService.java 1.0 8/13/2025
 *
 * Copyright (c) 2025 IUH.All rights reserved
 */

package iuh.fit.airsky.service;

/*
 * @description
 * @author : Nguyen Truong An
 * @date : 8/13/2025
 * @version 1.0
 */

import iuh.fit.airsky.dto.request.auth.*;
import iuh.fit.airsky.dto.response.AuthResponse;
import iuh.fit.airsky.dto.response.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse forgotPassword(ForgotPasswordRequest request);
    AuthResponse resendVerificationCode(ForgotPasswordRequest request);
    AuthResponse resetPassword(ResetPasswordRequest request);
    AuthResponse changePassword(ChangePasswordRequest request);
    AuthResponse verifyRegistration(VerifyOtpRequest request);
    UserResponse getUserByEmail(String email);

}