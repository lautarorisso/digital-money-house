package com.lautarorisso.users_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.users_service.exception.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final String LOGOUT_PATH = "/user/logout";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/users/register", "/auth/login", "/user/logout").permitAll()
            .requestMatchers("/error").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .bearerTokenResolver(bearerTokenResolver())
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
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

  @Bean
  BearerTokenResolver bearerTokenResolver() {
    DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
    return request -> LOGOUT_PATH.equals(request.getServletPath()) ? null : defaultResolver.resolve(request);
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
      List<GrantedAuthority> authorities = new ArrayList<>();
      authorities.addAll(scopeAuthoritiesConverter.convert(jwt));
      Object realmAccess = jwt.getClaims().get("realm_access");
      if (realmAccess instanceof Map<?, ?> realmMap) {
        Object roles = realmMap.get("roles");
        if (roles instanceof Collection<?> roleNames) {
          for (Object roleName : roleNames) {
            if (roleName instanceof String role) {
              authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
          }
        }
      }
      return authorities;
    };
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }
}