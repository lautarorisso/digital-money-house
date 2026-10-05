package com.lautarorisso.account_service.controller;

import com.lautarorisso.account_service.dto.CardResponse;
import com.lautarorisso.account_service.dto.CreateCardRequest;
import com.lautarorisso.account_service.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CardResponse create(@Valid @RequestBody CreateCardRequest request) {
    return cardService.createCard(request);
  }
}
