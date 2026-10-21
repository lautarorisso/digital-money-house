package com.lautarorisso.users_service.controller;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lautarorisso.users_service.exception.GlobalExceptionHandler;
import com.lautarorisso.users_service.exception.ValidationException;
import com.lautarorisso.users_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock AuthService authService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test
  void successfulAccessIsLoggedWithoutCredentialsAndFailuresAreNotLogged() throws Exception {
    var users = org.mockito.Mockito.mock(com.lautarorisso.users_service.repository.UserRepository.class);
    var keycloak = org.mockito.Mockito.mock(com.lautarorisso.users_service.client.KeycloakClient.class);
    var mvc = MockMvcBuilders.standaloneSetup(new AuthController(new AuthService(users, keycloak)))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
    var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AuthService.class);
    var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      org.mockito.Mockito.when(users.existsByEmail("audit@example.com")).thenReturn(true);
      org.mockito.Mockito.when(keycloak.login("audit@example.com", "private-password"))
          .thenReturn(new com.lautarorisso.users_service.dto.LoginResponse(
              "private-access-token", "private-refresh-token", "Bearer", 300));
      mvc.perform(post("/auth/login").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
          .content("{\"email\":\"audit@example.com\",\"password\":\"private-password\"}"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("private-access-token"));
      mvc.perform(post("/user/logout").header(HttpHeaders.AUTHORIZATION, "Bearer private-refresh-token"))
          .andExpect(status().isOk());
      org.junit.jupiter.api.Assertions.assertEquals(2, appender.list.size());
      org.junit.jupiter.api.Assertions.assertEquals("event=login", appender.list.get(0).getFormattedMessage());
      org.junit.jupiter.api.Assertions.assertEquals("event=logout", appender.list.get(1).getFormattedMessage());
      org.junit.jupiter.api.Assertions.assertTrue(appender.list.stream()
          .allMatch(event -> event.getLevel() == ch.qos.logback.classic.Level.INFO));
      appender.list.clear();
      org.mockito.Mockito.when(keycloak.login("audit@example.com", "private-password"))
          .thenThrow(new ValidationException("Invalid credentials"));
      doThrow(new ValidationException("Invalid or expired refresh token"))
          .when(keycloak).logout("private-refresh-token");
      mvc.perform(post("/auth/login").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
          .content("{\"email\":\"audit@example.com\",\"password\":\"private-password\"}"))
          .andExpect(status().isBadRequest());
      mvc.perform(post("/user/logout").header(HttpHeaders.AUTHORIZATION, "Bearer private-refresh-token"))
          .andExpect(status().isBadRequest());
      org.junit.jupiter.api.Assertions.assertTrue(appender.list.isEmpty());
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void logoutMapsInvalidRefreshTokenToBadRequest() throws Exception {
    doThrow(new ValidationException("Invalid or expired refresh token"))
        .when(authService).logout("invalid-token");

    mockMvc.perform(post("/user/logout").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"))
        .andExpect(jsonPath("$.path").value("/user/logout"));
  }
}
