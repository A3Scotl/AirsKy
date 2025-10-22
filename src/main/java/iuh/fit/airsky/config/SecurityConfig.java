
/*
 * @ (#) SecurityConfig.java 1.0 7/12/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved
 */
package iuh.fit.airsky.config;

import iuh.fit.airsky.exception.CustomAuthExceptionHandler;
import iuh.fit.airsky.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/*
 * @description: Security configuration for JWT-based authentication and CORS
 * @author: Nguyen Truong An
 * @date: 7/12/2025
 * @version: 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private static final String[] PUBLIC_ROUTES = {
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/verify-registration",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/google-login",
            "/api/v1/airports/**",
            "/api/v1/flights/**",
            "/api/v1/countries/**",
            "/api/v1/airlines/**",
            "/api/v1/bookings/**",
            "/api/v1/aircrafts/**",
            "/api/v1/deals/**",
            "/api/v1/blogs/**",
            "/api/v1/categories/**",
            "/api/v1/blog-likes/**",
            "/api/v1/travel-classes/**",
            "/api/v1/users/**",
            "/api/v1/checkins/**",
            "/api/v1/export/**",
            "/api/v1/reviews/**",
            "/api/v1/payments/**",
            "/api/v1/notifications/**",
            "/api/v1/gates/**",
            "/api/v1/ancillary-services/**",
            "/api/v1/boarding-passes/**",
            "/api/v1/loyalty/**",
            "/ws/**",
            "/websocket-test.html",
            "/api/v1/points-redemption/**"
    };
    private static final String[] PERMISION_ROUTES = {
            "/api/v1/auth/change-password",
            "/api/v1/auth/profile/me"
    };
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAuthExceptionHandler customHandler) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customHandler) // xử lý 401
                        .accessDeniedHandler(customHandler) // xử lý 403
                )
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .requestMatchers(PERMISION_ROUTES)
                        .hasAnyRole("BUSINESS", "CUSTOMER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}