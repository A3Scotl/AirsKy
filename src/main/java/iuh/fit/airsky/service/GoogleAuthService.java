package iuh.fit.airsky.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import iuh.fit.airsky.dto.response.AuthResponse;
import iuh.fit.airsky.enums.Role;
import iuh.fit.airsky.exception.GoogleAuthException;
import iuh.fit.airsky.exception.InvalidTokenException;
import iuh.fit.airsky.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String googleClientId;

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse loginWithGoogle(String idTokenString) {
        try {
            // Verify Google ID Token
            GoogleIdToken.Payload payload = verifyGoogleToken(idTokenString);

            // Extract user info
            String email = payload.getEmail();
            String firstName = (String) payload.get("given_name");
            String lastName = (String) payload.get("family_name");
            Boolean emailVerified = payload.getEmailVerified();

            // Find or create user
            User user = findOrCreateGoogleUser(email, firstName, lastName, emailVerified);

            // Check if user is active
            if (!user.isActive()) {
                throw new GoogleAuthException("Account is deactivated");
            }

            // Generate JWT token (same as normal login)
            AuthResponse authResponse = buildAuthResponse(user);

            log.info("Google login successful for user: {}", email);
            return authResponse;

        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage());
            throw new GoogleAuthException("Google authentication failed: " + e.getMessage());
        }
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new InvalidTokenException("Invalid Google ID token");
        }

        return idToken.getPayload();
    }

    private User findOrCreateGoogleUser(String email, String firstName, String lastName, Boolean emailVerified) {
        return userService.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, firstName, lastName, emailVerified));
    }

    private User createGoogleUser(String email, String firstName, String lastName, Boolean emailVerified) {
        // Tạo password phức tạp tuân thủ validation rules (uppercase, lowercase, digit, special char, min 8 chars)
        String securePassword = "GoogleAuth@" + System.currentTimeMillis();
        
        User newUser = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password(passwordEncoder.encode(securePassword)) // Password tuân thủ validation rules
                .isVerified(emailVerified != null ? emailVerified : true)
                .role(Role.CUSTOMER)
                .active(true)
                .deleted(false)
                .build();

        return userService.save(newUser);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new AuthResponse(accessToken, refreshToken);
    }
}