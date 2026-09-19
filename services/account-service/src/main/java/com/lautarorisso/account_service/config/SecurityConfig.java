package com.lautarorisso.account_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.account_service.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/error").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults())
            .authenticationEntryPoint((request, response, authException) -> writeApiError(
                response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                "Authentication required: missing or invalid token", request.getRequestURI())))
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
}