package com.lautarorisso.users_service.service;

import com.lautarorisso.users_service.client.AccountClient;
import com.lautarorisso.users_service.client.KeycloakClient;
import com.lautarorisso.users_service.dto.AccountResponse;
import com.lautarorisso.users_service.dto.CreateAccountRequest;
import com.lautarorisso.users_service.dto.RegisterRequest;
import com.lautarorisso.users_service.dto.RegisterResponse;
import com.lautarorisso.users_service.entity.RolEntity;
import com.lautarorisso.users_service.entity.UserEntity;
import com.lautarorisso.users_service.exception.ServiceUnavailableException;
import com.lautarorisso.users_service.exception.ValidationException;
import com.lautarorisso.users_service.repository.RolRepository;
import com.lautarorisso.users_service.repository.UserRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final KeycloakClient keycloakClient;
  private final AccountClient accountClient;
  private final RolRepository rolRepository;

  public RegisterResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ValidationException("Email is already registered");
    }
    RolEntity userRole = rolRepository.findByNombre("USER");
    if (userRole == null) {
      throw new IllegalStateException("Role 'USER' is missing: RolInitializer did not seed roles at startup");
    }
    String token = keycloakClient.getServiceAccountToken();
    String keycloakUserId = keycloakClient.createUser(token, request.email(), request.email(),
        request.nombre(), request.apellido(), request.password());
    UserEntity user = userRepository.save(
        new UserEntity(request.nombre(), request.apellido(), request.dni(), request.email(), request.telefono(),
            List.of(userRole)));
    AccountResponse account;
    try {
      account = accountClient.createAccount(new CreateAccountRequest(user.getId()));
    } catch (RuntimeException ex) {
      rollbackRegistration(user, token, keycloakUserId);
      throw new ServiceUnavailableException(
          "Account creation failed. The registration was rolled back, please try again.", ex);
    }
    return new RegisterResponse(user.getId(), user.getNombre(), user.getApellido(),
        user.getDni(), user.getEmail(), user.getTelefono(), account.cvu(), account.alias());
  }

  private void rollbackRegistration(UserEntity user, String keycloakToken, String keycloakUserId) {
    try {
      userRepository.delete(user);
    } catch (RuntimeException ex) {
      log.warn("Rollback: could not delete user {} from the database", user.getEmail(), ex);
    }
    try {
      keycloakClient.deleteUser(keycloakToken, keycloakUserId);
    } catch (RuntimeException ex) {
      log.warn("Rollback: could not delete user {} from Keycloak", user.getEmail(), ex);
    }
  }
}
