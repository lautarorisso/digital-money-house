package com.lautarorisso.users_service.service;

import com.lautarorisso.users_service.client.AccountClient;
import com.lautarorisso.users_service.client.KeycloakClient;
import com.lautarorisso.users_service.dto.AccountResponse;
import com.lautarorisso.users_service.dto.CreateAccountRequest;
import com.lautarorisso.users_service.dto.RegisterRequest;
import com.lautarorisso.users_service.dto.RegisterResponse;
import com.lautarorisso.users_service.dto.UserProfileResponse;
import com.lautarorisso.users_service.dto.UserUpdateRequest;
import com.lautarorisso.users_service.entity.RolEntity;
import com.lautarorisso.users_service.entity.UserEntity;
import com.lautarorisso.users_service.exception.ForbiddenException;
import com.lautarorisso.users_service.exception.ResourceNotFoundException;
import com.lautarorisso.users_service.exception.ServiceUnavailableException;
import com.lautarorisso.users_service.exception.ValidationException;
import com.lautarorisso.users_service.repository.RolRepository;
import com.lautarorisso.users_service.repository.UserRepository;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

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
            keycloakUserId, List.of(userRole)));
    AccountResponse account;
    try {
      account = accountClient.createAccount(new CreateAccountRequest(user.getId(), keycloakUserId));
    } catch (RuntimeException ex) {
      rollbackRegistration(user, token, keycloakUserId);
      throw new ServiceUnavailableException(
          "Account creation failed. The registration was rolled back, please try again.", ex);
    }
    return new RegisterResponse(user.getId(), user.getNombre(), user.getApellido(),
        user.getDni(), user.getEmail(), user.getTelefono(), account.cvu(), account.alias());
  }

  public UserProfileResponse getProfile(Long userId, String subject) {
    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (!subject.equals(user.getKeycloakSub())) {
      throw new ForbiddenException("You do not have access to this user");
    }
    return new UserProfileResponse(user.getId(), user.getNombre(), user.getApellido(), user.getDni(),
        user.getEmail(), user.getTelefono());
  }

  @Transactional
  public UserProfileResponse updateProfile(Long userId, String subject, UserUpdateRequest request) {
    UserEntity user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    if (!subject.equals(user.getKeycloakSub())) {
      throw new ForbiddenException("You do not have access to this user");
    }

    if (request.nombre() == null && request.apellido() == null && request.dni() == null
        && request.email() == null && request.telefono() == null && request.password() == null) {
      throw new ValidationException("At least one user field is required");
    }
    if (request.email() != null && !request.email().equals(user.getEmail())
        && userRepository.existsByEmail(request.email())) {
      throw new ValidationException("Email is already registered");
    }

    user.updateProfile(request.nombre(), request.apellido(), request.dni(), request.email(), request.telefono());
    try {
      userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException ex) {
      if (!isEmailUniqueViolation(ex)) {
        throw ex;
      }
      throw new ValidationException("Email is already registered");
    }

    String token = keycloakClient.getServiceAccountToken();
    keycloakClient.updateUser(token, user.getKeycloakSub(), user.getEmail(),
        user.getNombre(), user.getApellido(), request.password());

    return new UserProfileResponse(user.getId(), user.getNombre(), user.getApellido(), user.getDni(),
        user.getEmail(), user.getTelefono());
  }

  private boolean isEmailUniqueViolation(DataIntegrityViolationException exception) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
          && "uk_users_email".equalsIgnoreCase(constraintViolation.getConstraintName())) {
        return true;
      }
      if (cause.getMessage() != null && cause.getMessage().contains("uk_users_email")) {
        return true;
      }
    }
    return false;
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
