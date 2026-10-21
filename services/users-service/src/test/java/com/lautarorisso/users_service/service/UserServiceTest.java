package com.lautarorisso.users_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  UserRepository userRepository;
  @Mock
  KeycloakClient keycloakClient;
  @Mock
  AccountClient accountClient;
  @Mock
  RolRepository rolRepository;
  @Mock
  UserEntity savedUser;

  private UserService userService;
  private RegisterRequest request;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, keycloakClient, accountClient, rolRepository);
    request = new RegisterRequest("Lautaro", "Risso", 12345678L, "lautaro@example.com",
        "+541112345678", "securePass1");
  }

  @Test
  void registerReturnsAccountData() {
    when(rolRepository.findByNombre("USER")).thenReturn(new RolEntity("USER"));
    when(keycloakClient.getServiceAccountToken()).thenReturn("token");
    when(keycloakClient.createUser("token", request.email(), request.email(), request.nombre(),
        request.apellido(), request.password())).thenReturn("subject-123");
    when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
    when(savedUser.getId()).thenReturn(42L);
    when(savedUser.getNombre()).thenReturn(request.nombre());
    when(savedUser.getApellido()).thenReturn(request.apellido());
    when(savedUser.getDni()).thenReturn(request.dni());
    when(savedUser.getEmail()).thenReturn(request.email());
    when(savedUser.getTelefono()).thenReturn(request.telefono());
    when(accountClient.createAccount(new CreateAccountRequest(42L, "subject-123")))
        .thenReturn(new AccountResponse(9L, 42L, "1234567890123456789012", "casa.rio.sol"));

    RegisterResponse response = userService.register(request);

    assertEquals(42L, response.id());
    assertEquals(9L, new com.fasterxml.jackson.databind.ObjectMapper()
        .valueToTree(response).path("accountId").asLong());
    assertEquals("1234567890123456789012", response.cvu());
    assertEquals("casa.rio.sol", response.alias());
    verify(userRepository).save(any(UserEntity.class));
    verify(accountClient).createAccount(new CreateAccountRequest(42L, "subject-123"));
  }

  @Test
  void registerRejectsDuplicateEmail() {
    when(userRepository.existsByEmail(request.email())).thenReturn(true);

    assertThrows(ValidationException.class, () -> userService.register(request));

    verifyNoInteractions(keycloakClient, accountClient, rolRepository);
    verify(userRepository, never()).save(any());
  }

  @Test
  void registerCompensatesIdentityWhenUserPersistenceFails() {
    when(rolRepository.findByNombre("USER")).thenReturn(new RolEntity("USER"));
    when(keycloakClient.getServiceAccountToken()).thenReturn("token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("subject-123");
    when(userRepository.save(any(UserEntity.class)))
        .thenThrow(new org.springframework.dao.DataIntegrityViolationException("save failed"));

    ServiceUnavailableException exception = assertThrows(ServiceUnavailableException.class,
        () -> userService.register(request));

    assertEquals("User persistence failed. Identity cleanup was attempted.", exception.getMessage());
    verify(keycloakClient).deleteUser("token", "subject-123");
    verifyNoInteractions(accountClient);
    verify(userRepository, never()).delete(any());
  }

  @Test
  void registerPreservesUserWhenAccountOutcomeCannotBeConfirmed() {
    when(rolRepository.findByNombre("USER")).thenReturn(new RolEntity("USER"));
    when(keycloakClient.getServiceAccountToken()).thenReturn("token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("subject-123");
    when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
    when(savedUser.getId()).thenReturn(42L);
    when(accountClient.createAccount(new CreateAccountRequest(42L, "subject-123")))
        .thenThrow(new RuntimeException("response lost"));

    ServiceUnavailableException exception = assertThrows(ServiceUnavailableException.class,
        () -> userService.register(request));

    assertEquals("Account creation could not be confirmed. User and identity were preserved for recovery.",
        exception.getMessage());
    verify(accountClient, org.mockito.Mockito.times(2))
        .createAccount(new CreateAccountRequest(42L, "subject-123"));
    verify(userRepository, never()).delete(any());
    verify(keycloakClient, never()).deleteUser(any(), any());
  }

  @Test
  void registerRollsBackWhenAccountCreationFails() {
    when(rolRepository.findByNombre("USER")).thenReturn(new RolEntity("USER"));
    when(keycloakClient.getServiceAccountToken()).thenReturn("token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("subject-123");
    when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
    when(savedUser.getId()).thenReturn(42L);
    when(accountClient.createAccount(new CreateAccountRequest(42L, "subject-123")))
        .thenThrow(feign.FeignException.errorStatus("createAccount", feign.Response.builder()
            .status(400).reason("Bad Request").headers(java.util.Map.of())
            .request(feign.Request.create(feign.Request.HttpMethod.POST, "http://account-service/accounts",
                java.util.Map.of(), (byte[]) null, java.nio.charset.StandardCharsets.UTF_8, null))
            .build()));

    ServiceUnavailableException exception = assertThrows(ServiceUnavailableException.class,
        () -> userService.register(request));

    assertEquals("Account creation was rejected. Registration cleanup was attempted.",
        exception.getMessage());
    verify(userRepository).delete(savedUser);
    verify(keycloakClient).deleteUser("token", "subject-123");
    verify(accountClient).createAccount(new CreateAccountRequest(42L, "subject-123"));
  }

  @Test
  void registerRecoversLostResponseWithTheSameIdentityAndWithoutCleanup() {
    when(rolRepository.findByNombre("USER")).thenReturn(new RolEntity("USER"));
    when(keycloakClient.getServiceAccountToken()).thenReturn("token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("subject-123");
    when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
    when(savedUser.getId()).thenReturn(42L);
    when(accountClient.createAccount(new CreateAccountRequest(42L, "subject-123")))
        .thenThrow(new RuntimeException("response lost"))
        .thenReturn(new AccountResponse(9L, 42L, "1234567890123456789012", "casa.rio.sol"));

    RegisterResponse response = userService.register(request);

    assertEquals(42L, response.id());
    assertEquals(9L, response.accountId());
    assertEquals("1234567890123456789012", response.cvu());
    assertEquals("casa.rio.sol", response.alias());
    verify(accountClient, org.mockito.Mockito.times(2))
        .createAccount(new CreateAccountRequest(42L, "subject-123"));
    verify(userRepository, never()).delete(any());
    verify(keycloakClient, never()).deleteUser(any(), any());
  }

  @Test
  void identityCleanupFailureDoesNotHideThePersistenceFailure() {
    when(rolRepository.findByNombre("USER")).thenReturn(new RolEntity("USER"));
    when(keycloakClient.getServiceAccountToken()).thenReturn("token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("subject-123");
    var failure = new org.springframework.dao.DataIntegrityViolationException("save failed");
    when(userRepository.save(any(UserEntity.class))).thenThrow(failure);
    org.mockito.Mockito.doThrow(new IllegalStateException("cleanup unavailable"))
        .when(keycloakClient).deleteUser("token", "subject-123");

    ServiceUnavailableException exception = assertThrows(ServiceUnavailableException.class,
        () -> userService.register(request));

    assertEquals("User persistence failed. Identity cleanup was attempted.", exception.getMessage());
    org.junit.jupiter.api.Assertions.assertSame(failure, exception.getCause());
    verifyNoInteractions(accountClient);
  }
}
