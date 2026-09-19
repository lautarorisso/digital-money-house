package com.lautarorisso.api_gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String LOGOUT_PATH = "/users-service/user/logout";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/users-service/users/register", "/users-service/auth/login", "/users-service/user/logout").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(bearerTokenResolver())
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint((request, response, authException) -> writeApiError(
                    response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                    "Authentication required: missing or invalid token", request.getRequestURI()))
            )
            .exceptionHandling(ex -> ex.accessDeniedHandler((request, response, accessDeniedException) -> writeApiError(
                response, objectMapper, HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                "Access denied: insufficient permissions", request.getRequestURI())));
        return http.build();
    }

    private void writeApiError(HttpServletResponse response, ObjectMapper objectMapper,
            int status, String error, String message, String path) throws IOException {
        ApiError apiError = new ApiError(LocalDateTime.now(), status, error, message, path);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), apiError);
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return request -> LOGOUT_PATH.equals(request.getServletPath()) ? null : defaultResolver.resolve(request);
    }
}
