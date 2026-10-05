package com.lautarorisso.users_service.controller;

import com.lautarorisso.users_service.dto.UserProfileResponse;
import com.lautarorisso.users_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  @GetMapping("/{id}")
  public UserProfileResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
    return userService.getProfile(id, jwt.getSubject());
  }
}
