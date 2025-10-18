package iuh.fit.airsky.config;

import iuh.fit.airsky.model.User;
import iuh.fit.airsky.repository.UserRepository;
import iuh.fit.airsky.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            log.info("🔌 WebSocket handshake starting...");

            // Lấy token từ query parameter
            String query = request.getURI().getQuery();
            log.info("Query parameters: {}", query);

            String token = null;

            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        token = param.substring(6); // Remove "token="
                        log.info("Found token parameter");
                        break;
                    }
                }
            }

            if (token == null) {
                log.warn("❌ No token found in WebSocket handshake");
                return false;
            }

            log.info("Validating JWT token...");
            boolean isValid = jwtUtil.validateToken(token);

            if (!isValid) {
                log.warn("❌ JWT token validation failed");
                return false;
            }

            log.info("✅ JWT token is valid, extracting username...");
            String email = jwtUtil.getEmailFromToken(token);
            log.info("User email from token: {}", email);

            // Tìm user theo email
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.warn("❌ User not found for email: {}", email);
                return false;
            }

            // Set authentication vào attributes để WebSocket handler sử dụng
            attributes.put("user", user);
            attributes.put("userId", user.getId());

            log.info("✅ WebSocket handshake successful for user: {} (ID: {})", email, user.getId());
            return true;

        } catch (Exception e) {
            log.error("❌ Error during WebSocket handshake: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Cleanup sau handshake nếu cần
    }
}