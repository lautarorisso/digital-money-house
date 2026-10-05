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
