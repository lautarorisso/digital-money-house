package com.lautarorisso.account_service.controller;

import com.lautarorisso.account_service.dto.AccountResponse;
import com.lautarorisso.account_service.dto.AccountDetailResponse;
import com.lautarorisso.account_service.dto.CardResponse;
import com.lautarorisso.account_service.dto.CreateAccountRequest;
import com.lautarorisso.account_service.dto.CreateCardRequest;
import com.lautarorisso.account_service.dto.CreateTransferenceRequest;
import com.lautarorisso.account_service.dto.TransactionResponse;
import com.lautarorisso.account_service.dto.UpdateAccountRequest;
import com.lautarorisso.account_service.dto.InternalTransferRequest;
import com.lautarorisso.account_service.dto.TransferRecipientResponse;
import com.lautarorisso.account_service.service.InternalTransferService;
import com.lautarorisso.account_service.service.AccountService;
import com.lautarorisso.account_service.service.CardService;
import com.lautarorisso.account_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@lombok.extern.slf4j.Slf4j
@Tag(name = "Cuentas", description = "Cuentas, movimientos y transferencias")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Solicitud inválida")
@ApiResponse(responseCode = "401", description = "JWT ausente o inválido")
@ApiResponse(responseCode = "403", description = "Sin permisos sobre la cuenta")
@ApiResponse(responseCode = "404", description = "Recurso no encontrado")
@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
public class AccountController {

  private final AccountService accountService;
  private final CardService cardService;
  private final TransactionService transactionService;
  private final InternalTransferService internalTransferService;

  @Operation(summary = "Transferir dinero a otra cuenta")
  @ApiResponse(responseCode = "200", description = "Transferencia realizada")
  @ApiResponse(responseCode = "410", description = "Saldo insuficiente")
  @PostMapping("/{id}/transferences")
  public Map<String, String> transfer(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") Long id,
      @Valid @RequestBody InternalTransferRequest request) {
    internalTransferService.transfer(id, jwt.getSubject(), request);
    log.info("event=transfer accountId={} amount={}", id, request.amount());
    return Map.of("message", "ok");
  }

  @Operation(summary = "Consultar destinatarios de transferencias")
  @ApiResponse(responseCode = "200", description = "Destinatarios de transferencias")
  @GetMapping("/{id}/transferences")
  public List<TransferRecipientResponse> recipients(
      @AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
    return internalTransferService.recipients(id, jwt.getSubject());
  }

  @Operation(summary = "Crear o recuperar una cuenta", description = "Requiere el cliente de servicio autorizado. Repetir el mismo userId y ownerSub devuelve la cuenta existente; otra identidad recibe 403.")
  @ApiResponse(responseCode = "201", description = "Cuenta creada o recuperada sin duplicados")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse create(@AuthenticationPrincipal Jwt jwt,
      @RequestBody(required = false) CreateAccountRequest request) {
    Long userId = request == null ? null : request.userId();
    String ownerSub = request == null ? null : request.ownerSub();
    return accountService.createAccount(jwt.getClaimAsString("azp"), userId, ownerSub);
  }

  @Operation(summary = "Consultar una cuenta")
  @ApiResponse(responseCode = "200", description = "Detalle de la cuenta")
  @GetMapping("/{id}")
  public AccountDetailResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id) {
    return accountService.getAccountDetail(id, jwt.getSubject());
  }

  @Operation(summary = "Consultar transacciones de una cuenta")
  @ApiResponse(responseCode = "200", description = "Transacciones de la cuenta")
  @GetMapping("/{id}/transactions")
  public List<TransactionResponse> getTransactions(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") Long id) {
    return transactionService.getTransactions(id, jwt.getSubject());
  }

  @Operation(summary = "Actualizar el alias de una cuenta")
  @ApiResponse(responseCode = "201", description = "Alias actualizado")
  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.CREATED)
  public AccountDetailResponse updateById(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
      @Valid @RequestBody UpdateAccountRequest request) {
    return accountService.updateAlias(id, jwt.getSubject(), request.alias());
  }

  @Operation(summary = "Consultar actividad con filtros opcionales")
  @ApiResponse(responseCode = "200", description = "Actividad de la cuenta")
  @GetMapping("/{id}/activity")
  public List<TransactionResponse> getActivity(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("id") Long id,
      @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(name = "type", required = false) String type,
      @RequestParam(name = "minAmount", required = false) BigDecimal minAmount,
      @RequestParam(name = "maxAmount", required = false) BigDecimal maxAmount) {
    return transactionService.getActivity(id, jwt.getSubject(), from, to, type, minAmount, maxAmount);
  }

  @Operation(summary = "Consultar el detalle de un movimiento")
  @ApiResponse(responseCode = "200", description = "Detalle del movimiento")
  @GetMapping("/{accountId}/activity/{transferId}")
  public TransactionResponse getActivityDetail(@AuthenticationPrincipal Jwt jwt,
      @PathVariable("accountId") Long accountId, @PathVariable("transferId") Long transferId) {
    return transactionService.getActivityDetail(accountId, transferId, jwt.getSubject());
  }

  @Operation(summary = "Ingresar dinero desde una tarjeta asociada")
  @ApiResponse(responseCode = "201", description = "Depósito registrado")
  @PostMapping("/{id}/deposits")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionResponse createTransference(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
      @Valid @RequestBody CreateTransferenceRequest request) {
    TransactionResponse response = transactionService.createTransference(id, jwt.getSubject(), request);
    log.info("event=deposit accountId={} transactionId={} amount={}", id, response.id(), response.amount());
    return response;
  }

  @Operation(summary = "Asociar una tarjeta a una cuenta")
  @ApiResponse(responseCode = "201", description = "Tarjeta asociada")
  @PostMapping("/{id}/cards")
  @ResponseStatus(HttpStatus.CREATED)
  public CardResponse associateCard(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") Long id,
      @Valid @RequestBody CreateCardRequest request) {
    return cardService.associateCard(id, jwt.getSubject(), request);
  }
}
