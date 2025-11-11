package iuh.fit.airsky.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;

@Configuration
@EnableWebSocketMessageBroker
@EnableAsync
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Client sẽ lắng nghe trên các topic bắt đầu bằng /topic hoặc /queue
        config.enableSimpleBroker("/topic", "/queue");
        // Các message từ client gửi đến server sẽ có prefix là /app
        config.setApplicationDestinationPrefixes("/app");
        // Cấu hình để gửi message đến user cụ thể (vd: /user/{userId}/queue/notifications)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client kết nối WebSocket tới
        // withSockJS() để hỗ trợ các trình duyệt không hỗ trợ WebSocket thuần
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép tất cả các origin, cần cấu hình chặt chẽ hơn trên production
                .addInterceptors(jwtHandshakeInterceptor) // ✅ Thêm JWT interceptor
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                // .setHeartbeatValue(new long[]{10000, 20000}) // Removed - not available in Spring Boot 3.x
                .setDisconnectDelay(30 * 1000) // 30 seconds disconnect delay
                .setSessionCookieNeeded(false); // Disable session cookies
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Cấu hình SockJS để tránh fallback sang JSONP transport gây lỗi 404
        registration.setMessageSizeLimit(64 * 1024); // 64KB
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB
        registration.setSendTimeLimit(10 * 1000); // 10 seconds
    }
}