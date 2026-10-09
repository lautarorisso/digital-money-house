package com.lautarorisso.account_service.controller;

import com.lautarorisso.account_service.dto.AccountResponse;
import com.lautarorisso.account_service.dto.AccountDetailResponse;
import com.lautarorisso.account_service.dto.CardResponse;
import com.lautarorisso.account_service.dto.CreateAccountRequest;
import com.lautarorisso.account_service.dto.CreateCardRequest;
import com.lautarorisso.account_service.dto.TransactionResponse;
import com.lautarorisso.account_service.dto.UpdateAccountRequest;
import com.lautarorisso.account_service.service.AccountService;
import com.lautarorisso.account_service.service.CardService;
import com.lautarorisso.account_service.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  private final CardService cardService;
  private final TransactionService transactionService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse create(@AuthenticationPrincipal Jwt jwt,
      @RequestBody(required = false) CreateAccountRequest request) {
    Long userId = request == null ? null : request.userId();
    String ownerSub = request == null ? null : request.ownerSub();
    return accountService.createAccount(jwt.getClaimAsString("azp"), userId, ownerSub);
  }

  @GetMapping("/{id}")
  public AccountDetailResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
    return accountService.getAccountDetail(id, jwt.getSubject());
  }

  @GetMapping("/{id}/transactions")
  public List<TransactionResponse> getTransactions(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") Long id) {
    return transactionService.getTransactions(id, jwt.getSubject());
  }

  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.CREATED)
  public AccountDetailResponse updateById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
      @Valid @RequestBody UpdateAccountRequest request) {
    return accountService.updateAlias(id, jwt.getSubject(), request.alias());
  }

  @PostMapping("/{id}/cards")
  @ResponseStatus(HttpStatus.CREATED)
  public CardResponse associateCard(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
      @Valid @RequestBody CreateCardRequest request) {
    return cardService.associateCard(id, jwt.getSubject(), request);
  }
}
