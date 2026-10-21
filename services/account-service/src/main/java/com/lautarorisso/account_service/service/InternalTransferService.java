package com.lautarorisso.account_service.service;

import com.lautarorisso.account_service.dto.InternalTransferRequest;
import com.lautarorisso.account_service.dto.TransferRecipientResponse;
import com.lautarorisso.account_service.entity.AccountEntity;
import com.lautarorisso.account_service.entity.TransactionEntity;
import com.lautarorisso.account_service.exception.*;
import com.lautarorisso.account_service.repository.AccountRepository;
import com.lautarorisso.account_service.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalTransferService {
  private static final BigDecimal MAX_BALANCE = new BigDecimal("9999999999.99");
  private final AccountRepository accounts;
  private final TransactionRepository transactions;

  @Transactional
  public void transfer(Long sourceId, String subject, InternalTransferRequest request) {
    checkOwner(subject, accounts.findOwnerSubById(sourceId)
        .orElseThrow(() -> new ValidationException("Account not found")));
    BigDecimal amount = request.amount();
    if (amount == null || amount.signum() <= 0 || amount.scale() > 2
        || amount.compareTo(MAX_BALANCE) > 0) {
      throw new ValidationException("Invalid amount");
    }
    String destination = request.destination();
    if (destination == null || destination.isBlank()) {
      throw new ValidationException("Invalid destination");
    }
    Long destinationId = accounts.findIdByDestination(destination)
        .orElseThrow(() -> new ValidationException("Invalid destination"));
    if (sourceId.equals(destinationId)) {
      throw new ValidationException("Cannot transfer to the same account");
    }
    AccountEntity first = lock(Math.min(sourceId, destinationId));
    AccountEntity second = lock(Math.max(sourceId, destinationId));
    AccountEntity source = sourceId < destinationId ? first : second;
    AccountEntity target = sourceId < destinationId ? second : first;
    checkOwner(subject, source.getOwnerSub());
    if (!destination.equals(target.getCvu()) && !destination.equals(target.getAlias())) {
      throw new ValidationException("Invalid destination");
    }
    if (source.getBalance().compareTo(amount) < 0) {
      throw new InsufficientFundsException();
    }
    if (target.getBalance().add(amount).compareTo(MAX_BALANCE) > 0) {
      throw new ValidationException("Amount exceeds account balance limit");
    }
    source.debit(amount);
    target.credit(amount);
    LocalDateTime now = LocalDateTime.now();
    transactions.save(new TransactionEntity(sourceId, amount.negate(), "DEBIT",
        "Transfer to " + target.getAlias(), now, source.getBalance(), destinationId));
    transactions.save(new TransactionEntity(destinationId, amount, "CREDIT",
        "Transfer from " + source.getAlias(), now, target.getBalance(), sourceId));
  }

  @Transactional(readOnly = true)
  public List<TransferRecipientResponse> recipients(Long sourceId, String subject) {
    checkOwner(subject, accounts.findOwnerSubById(sourceId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found")));
    List<Long> ids = transactions.findRecentRecipientIds(sourceId, PageRequest.of(0, 5));
    var recipients = accounts.findAllById(ids).stream()
        .collect(Collectors.toMap(AccountEntity::getId, Function.identity()));
    return ids.stream().map(recipients::get)
        .map(account -> new TransferRecipientResponse(account.getCvu(), account.getAlias())).toList();
  }

  private AccountEntity lock(Long id) {
    return accounts.findByIdForUpdate(id)
        .orElseThrow(() -> new ValidationException("Account not found"));
  }

  private void checkOwner(String subject, String owner) {
    if (!subject.equals(owner)) {
      throw new ForbiddenException("You do not have access to this account");
    }
  }
}
