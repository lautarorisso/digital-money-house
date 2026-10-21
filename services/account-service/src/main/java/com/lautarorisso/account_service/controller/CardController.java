package com.lautarorisso.account_service.controller;

import com.lautarorisso.account_service.dto.CardResponse;
import com.lautarorisso.account_service.dto.CreateCardRequest;
import com.lautarorisso.account_service.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Tarjetas")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Solicitud inválida")
@ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
@ApiResponse(responseCode = "403", description = "Sin permisos sobre la cuenta")
@ApiResponse(responseCode = "404", description = "Tarjeta o cuenta no encontrada")
@RequiredArgsConstructor
@RestController
@RequestMapping("/cards")
public class CardController {

  private final CardService cardService;

  @Operation(summary = "Listar tarjetas de una cuenta")
  @ApiResponse(responseCode = "200", description = "Tarjetas de la cuenta")
  @GetMapping("/accounts/{id}/cards")
  public List<CardResponse> getByAccount(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
    return cardService.getCardsByAccount(id, jwt.getSubject());
  }

  @Operation(summary = "Consultar una tarjeta de la cuenta")
  @ApiResponse(responseCode = "200", description = "Detalle de la tarjeta")
  @GetMapping("/accounts/{accountId}/cards/{cardId}")
  public CardResponse getById(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("accountId") Long accountId, @PathVariable("cardId") Long cardId) {
    return cardService.getCardByAccount(accountId, cardId, jwt.getSubject());
  }

  @Operation(summary = "Eliminar una tarjeta de la cuenta")
  @ApiResponse(responseCode = "200", description = "Tarjeta eliminada")
  @DeleteMapping("/accounts/{accountId}/cards/{cardId}")
  @ResponseStatus(HttpStatus.OK)
  public void delete(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("accountId") Long accountId, @PathVariable("cardId") Long cardId) {
    cardService.deleteCard(accountId, cardId, jwt.getSubject());
  }

  @Operation(summary = "Crear una tarjeta", description = "Requiere el cliente de servicio autorizado")
  @ApiResponse(responseCode = "201", description = "Tarjeta creada")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CardResponse create(@Valid @RequestBody CreateCardRequest request) {
    return cardService.createCard(request);
  }
}
