package com.lautarorisso.users_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lautarorisso.users_service.client.AccountClient;
import com.lautarorisso.users_service.client.KeycloakClient;
import com.lautarorisso.users_service.dto.AccountResponse;
import com.lautarorisso.users_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:users;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused",
    "keycloak.base-url=http://localhost",
    "keycloak.realm=test",
    "keycloak.backend-client-id=test",
    "keycloak.backend-client-secret=test",
    "keycloak.frontend-client-id=test"
})
@AutoConfigureMockMvc
class RegistrationIntegrationTest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  UserRepository userRepository;
  @MockitoBean
  KeycloakClient keycloakClient;
  @MockitoBean
  AccountClient accountClient;

  @Test
  void documentationIsPublicAndDescribesGatewayBearerAccess() throws Exception {
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.servers[0].url").value("/users-service"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
        .andExpect(jsonPath("$.paths['/auth/login'].post.security").doesNotExist())
        .andExpect(jsonPath("$.paths['/users/register'].post.security").doesNotExist())
        .andExpect(jsonPath("$.paths['/user/logout'].post.security[0].refreshToken").isArray())
        .andExpect(jsonPath("$.paths['/user/logout'].post.security[0].bearerAuth").doesNotExist())
        .andExpect(jsonPath("$.paths['/users/{id}'].get.responses['200']").exists())
        .andExpect(jsonPath("$.components.securitySchemes.refreshToken.type").value("http"))
        .andExpect(jsonPath("$.components.securitySchemes.refreshToken.scheme").value("bearer"))
        .andExpect(jsonPath("$.components.securitySchemes.refreshToken.bearerFormat").value("JWT"))
        .andExpect(jsonPath("$.paths['/user/logout'].post.parameters[?(@.name == 'Authorization')]").doesNotExist());
    mockMvc.perform(post("/user/logout"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Refresh token is required"));
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs/swagger-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("/users-service/v3/api-docs"));
    mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/users/1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerPersistsUserThroughTheApi() throws Exception {
    when(keycloakClient.getServiceAccountToken()).thenReturn("service-token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("subject-123");
    when(accountClient.createAccount(any()))
        .thenReturn(new AccountResponse(7L, 1L, "1234567890123456789012", "casa.rio.sol"));

    mockMvc.perform(post("/users/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"nombre":"Lautaro","apellido":"Risso","dni":12345678,
             "email":"lautaro@example.com","telefono":"+541112345678","password":"securePass1"}
            """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.greaterThan(0)))
        .andExpect(jsonPath("$.accountId").value(7))
        .andExpect(jsonPath("$.cvu").value("1234567890123456789012"))
        .andExpect(jsonPath("$.alias").value("casa.rio.sol"));

    org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsByEmail("lautaro@example.com"));
  }

  @Test
  void registrationRecoversLostAccountResponseThroughTheApi() throws Exception {
    when(keycloakClient.getServiceAccountToken()).thenReturn("service-token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("recovered-sub");
    when(accountClient.createAccount(any())).thenThrow(new IllegalStateException("response lost"))
        .thenReturn(new AccountResponse(8L, 1L, "1234567890123456789013", "casa.rio.luna"));

    mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content("""
        {"nombre":"Lautaro","apellido":"Risso","dni":12345679,
         "email":"recovered@example.com","password":"securePass1"}
        """))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.greaterThan(0)))
        .andExpect(jsonPath("$.accountId").value(8))
        .andExpect(jsonPath("$.cvu").value("1234567890123456789013"))
        .andExpect(jsonPath("$.alias").value("casa.rio.luna"));
    org.mockito.Mockito.verify(accountClient, org.mockito.Mockito.times(2)).createAccount(any());
    org.mockito.Mockito.verify(keycloakClient, org.mockito.Mockito.never()).deleteUser(any(), any());
  }

  @Test
  void unconfirmedRegistrationReturns503AndPreservesTheIdentityThroughTheApi() throws Exception {
    when(keycloakClient.getServiceAccountToken()).thenReturn("service-token");
    when(keycloakClient.createUser(any(), any(), any(), any(), any(), any())).thenReturn("pending-sub");
    when(accountClient.createAccount(any())).thenThrow(new IllegalStateException("unavailable"));
    String body = """
        {"nombre":"Lautaro","apellido":"Risso","dni":12345680,
         "email":"pending@example.com","password":"securePass1"}
        """;

    mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.message").value(
            "Account creation could not be confirmed. User and identity were preserved for recovery."));
    mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Email is already registered"));
    org.mockito.Mockito.verify(accountClient, org.mockito.Mockito.times(2)).createAccount(any());
    org.mockito.Mockito.verify(keycloakClient, org.mockito.Mockito.never()).deleteUser(any(), any());
  }
}
