package iuh.fit.airsky.security;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.expiration}")
    private long expiration;

    @Value("${jwt.refresh.expiration}")
    private long resetPasswordExpiration;

    @PostConstruct
    public void validateSecretKey() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret key is not set. Please configure JWT_SECRET environment variable.");
        }
    }

    /**
     * Generate a JWT token for the user.
     */
    public String generateToken(String email, String role) {
        logger.debug("Generating token for email: {}, role: {}", email, role);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * Generate a JWT token for password reset.
     */
    public String generateResetPasswordToken(String email) {
        logger.debug("Generating reset password token for email: {}", email);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + resetPasswordExpiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    /**
     * Extract email from the JWT token.
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extract role from the JWT token.
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Validate the JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            logger.debug("Token validated for email: {}", claims.getSubject());
            return true;
        } catch (ExpiredJwtException ex) {
            logger.warn("Token expired: {}", ex.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to parse claims from the JWT token.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}