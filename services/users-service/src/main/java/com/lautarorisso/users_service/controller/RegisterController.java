package com.lautarorisso.users_service.controller;

import com.lautarorisso.users_service.dto.RegisterRequest;
import com.lautarorisso.users_service.dto.RegisterResponse;
import com.lautarorisso.users_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Registro")
@RequiredArgsConstructor
@RestController
public class RegisterController {

  private final UserService userService;

  @Operation(summary = "Registrar un usuario y crear su cuenta")
  @ApiResponse(responseCode = "201", description = "Usuario registrado")
  @ApiResponse(responseCode = "400", description = "Datos de registro inválidos")
  @ApiResponse(responseCode = "409", description = "Usuario ya registrado")
  @PostMapping("/users/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    return userService.register(request);
  }
}
