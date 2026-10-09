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
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.cvu").value("1234567890123456789012"))
        .andExpect(jsonPath("$.alias").value("casa.rio.sol"));

    org.junit.jupiter.api.Assertions.assertTrue(userRepository.existsByEmail("lautaro@example.com"));
  }
}
