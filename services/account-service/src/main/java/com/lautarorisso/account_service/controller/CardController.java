package com.lautarorisso.account_service.controller;

import com.lautarorisso.account_service.dto.CardResponse;
import com.lautarorisso.account_service.dto.CreateCardRequest;
import com.lautarorisso.account_service.service.CardService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor
@RestController
@RequestMapping("/cards")
public class CardController {

  private final CardService cardService;

  @GetMapping("/accounts/{id}/cards")
  public List<CardResponse> getByAccount(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
    return cardService.getCardsByAccount(id, jwt.getSubject());
  }

  @GetMapping("/accounts/{accountId}/cards/{cardId}")
  public CardResponse getById(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("accountId") Long accountId, @PathVariable("cardId") Long cardId) {
    return cardService.getCardByAccount(accountId, cardId, jwt.getSubject());
  }

  @DeleteMapping("/accounts/{accountId}/cards/{cardId}")
  @ResponseStatus(HttpStatus.OK)
  public void delete(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("accountId") Long accountId, @PathVariable("cardId") Long cardId) {
    cardService.deleteCard(accountId, cardId, jwt.getSubject());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CardResponse create(@Valid @RequestBody CreateCardRequest request) {
    return cardService.createCard(request);
  }
}
