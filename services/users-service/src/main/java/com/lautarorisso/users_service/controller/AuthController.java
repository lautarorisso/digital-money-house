package com.lautarorisso.users_service.controller;

import com.lautarorisso.users_service.dto.LoginRequest;
import com.lautarorisso.users_service.dto.LoginResponse;
import com.lautarorisso.users_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación")
@RequiredArgsConstructor
@RestController
public class AuthController {

  private static final String BEARER_PREFIX = "Bearer ";

  private final AuthService authService;

  @Operation(summary = "Iniciar sesión y obtener tokens de acceso y renovación")
  @ApiResponse(responseCode = "200", description = "Sesión iniciada")
  @ApiResponse(responseCode = "400", description = "Credenciales o solicitud inválidas")
  @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
  @PostMapping("/auth/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "refreshToken")
  @Operation(summary = "Cerrar sesión", description = "Enviar el refresh token en Authorization: Bearer <refresh token>; no el access token")
  @ApiResponse(responseCode = "200", description = "Sesión cerrada")
  @ApiResponse(responseCode = "400", description = "Refresh token ausente, inválido o vencido")
  @PostMapping("/user/logout")
  public ResponseEntity<Void> logout(
      @Parameter(hidden = true)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    authService.logout(extractToken(authorization));
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  private String extractToken(String authorization) {
    if (authorization == null || authorization.isBlank()) {
      return null;
    }
    String value = authorization.trim();
    if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
      return value.substring(BEARER_PREFIX.length()).trim();
    }
    return value;
  }
}