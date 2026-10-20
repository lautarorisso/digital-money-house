package com.lautarorisso.users_service.controller;

import com.lautarorisso.users_service.dto.UserProfileResponse;
import com.lautarorisso.users_service.dto.UserUpdateRequest;
import com.lautarorisso.users_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuarios")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Solicitud inválida")
@ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
@ApiResponse(responseCode = "403", description = "Sin permisos sobre el usuario")
@ApiResponse(responseCode = "404", description = "Usuario no encontrado")
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  @Operation(summary = "Consultar el perfil de usuario")
  @ApiResponse(responseCode = "200", description = "Perfil del usuario")
  @GetMapping("/{id}")
  public UserProfileResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
    return userService.getProfile(id, jwt.getSubject());
  }

  @Operation(summary = "Actualizar el perfil de usuario")
  @ApiResponse(responseCode = "201", description = "Perfil actualizado")
  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.CREATED)
  public UserProfileResponse updateById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
      @Valid @RequestBody UserUpdateRequest request) {
    return userService.updateProfile(id, jwt.getSubject(), request);
  }
}
