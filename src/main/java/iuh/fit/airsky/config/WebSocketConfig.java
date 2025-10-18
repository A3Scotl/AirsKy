package iuh.fit.airsky.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
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
                .withSockJS();
    }
}