package iuh.fit.airsky.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.airsky.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZonedDateTime;

@Component
public class CustomAuthExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private void writeError(HttpServletResponse response, HttpStatus status, String message, String error, String path) throws IOException {
        ApiResponse<Object> apiResponse = new ApiResponse<>(
                false,
                message,
                null,
                error,
                ZonedDateTime.now(),
                path
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        new ObjectMapper().writeValue(response.getOutputStream(), apiResponse);
    }

    // Xử lý khi chưa đăng nhập hoặc token sai
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "INVALID_TOKEN_OR_NOT_LOGGED_IN", request.getRequestURI());
    }

    // Xử lý khi không đủ quyền
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "Access Denied", "FORBIDDEN", request.getRequestURI());
    }
}
