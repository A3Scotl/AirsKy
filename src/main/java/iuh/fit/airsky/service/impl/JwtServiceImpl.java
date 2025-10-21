package iuh.fit.airsky.service.impl;

import iuh.fit.airsky.security.JwtUtil;
import iuh.fit.airsky.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtUtil jwtUtil;

    @Override
    public String generateAccessToken(String email, String role) {
        return jwtUtil.generateToken(email, role);
    }

    @Override
    public String generateRefreshToken(String email) {
        return jwtUtil.generateResetPasswordToken(email);
    }

    @Override
    public String generateAccessToken(Long userId, String email, String role) {
        return jwtUtil.generateToken(userId, email, role);
    }
}