package com.lautarorisso.account_service.controller;

import com.lautarorisso.account_service.dto.AccountResponse;
import com.lautarorisso.account_service.dto.CreateAccountRequest;
import com.lautarorisso.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
public class AccountController {

  private final AccountService accountService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse create(@AuthenticationPrincipal Jwt jwt,
      @RequestBody(required = false) CreateAccountRequest request) {
    Long userId = request == null ? null : request.userId();
    return accountService.createAccount(jwt.getClaimAsString("azp"), userId);
  }
}