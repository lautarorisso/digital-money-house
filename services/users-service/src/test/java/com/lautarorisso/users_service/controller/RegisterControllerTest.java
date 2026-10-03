package com.lautarorisso.users_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lautarorisso.users_service.dto.RegisterResponse;
import com.lautarorisso.users_service.exception.GlobalExceptionHandler;
import com.lautarorisso.users_service.exception.ValidationException;
import com.lautarorisso.users_service.service.UserService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

  @Mock
  UserService userService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new RegisterController(userService))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test
  void registerReturnsCreatedContract() throws Exception {
    when(userService.register(any())).thenReturn(new RegisterResponse(42L, "Lautaro", "Risso",
        12345678L, "lautaro@example.com", "+541112345678", "1234567890123456789012", "casa.rio.sol"));

    mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(42))
        .andExpect(jsonPath("$.cvu").value("1234567890123456789012"))
        .andExpect(jsonPath("$.alias").value("casa.rio.sol"));
  }

  @Test
  void registerMapsBusinessErrorToBadRequestApiError() throws Exception {
    when(userService.register(any())).thenThrow(new ValidationException("Email is already registered"));

    mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Email is already registered"))
        .andExpect(jsonPath("$.path").value("/users/register"));
  }

  private String validRequest() throws Exception {
    return new ObjectMapper().writeValueAsString(Map.of(
        "nombre", "Lautaro", "apellido", "Risso", "dni", 12345678,
        "email", "lautaro@example.com", "telefono", "+541112345678", "password", "securePass1"));
  }
}
