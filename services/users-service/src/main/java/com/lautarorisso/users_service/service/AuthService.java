package com.lautarorisso.users_service.service;

import com.lautarorisso.users_service.client.KeycloakClient;
import com.lautarorisso.users_service.dto.LoginRequest;
import com.lautarorisso.users_service.dto.LoginResponse;
import com.lautarorisso.users_service.exception.ResourceNotFoundException;
import com.lautarorisso.users_service.exception.ValidationException;
import com.lautarorisso.users_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

  private final UserRepository userRepository;
  private final KeycloakClient keycloakClient;

  public LoginResponse login(LoginRequest request) {
    if (!userRepository.existsByEmail(request.email())) {
      throw new ResourceNotFoundException("User not found");
    }
    return keycloakClient.login(request.email(), request.password());
  }

  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new ValidationException("Refresh token is required");
    }
    keycloakClient.logout(refreshToken);
  }
}