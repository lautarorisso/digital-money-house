package com.lautarorisso.account_service.service;

import com.lautarorisso.account_service.dto.CreateTransferenceRequest;
import com.lautarorisso.account_service.dto.TransactionResponse;
import com.lautarorisso.account_service.entity.AccountEntity;
import com.lautarorisso.account_service.entity.TransactionEntity;
import com.lautarorisso.account_service.exception.ForbiddenException;
import com.lautarorisso.account_service.exception.ResourceNotFoundException;
import com.lautarorisso.account_service.exception.ValidationException;
import com.lautarorisso.account_service.mapper.TransactionMapper;
import com.lautarorisso.account_service.repository.AccountRepository;
import com.lautarorisso.account_service.repository.CardRepository;
import com.lautarorisso.account_service.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransactionService {

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;
  private final CardRepository cardRepository;

  @Transactional
  public TransactionResponse createTransference(Long accountId, String subject, CreateTransferenceRequest request) {
    AccountEntity account = accountRepository.findByIdForUpdate(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    cardRepository.findByIdAndAccountId(request.cardId(), accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Card not found in this account"));
    if (account.getBalance().add(request.amount()).compareTo(new BigDecimal("9999999999.99")) > 0) {
      throw new ValidationException("Amount exceeds account balance limit");
    }
    account.credit(request.amount());
    TransactionEntity transaction = transactionRepository.save(new TransactionEntity(accountId,
        request.amount(), "CREDIT", "Card deposit", LocalDateTime.now(), account.getBalance()));
    return transactionMapper.toResponse(transaction);
  }

  @Transactional(readOnly = true)
  public List<TransactionResponse> getActivity(Long accountId, String subject, LocalDate from,
      LocalDate to, String type, BigDecimal minAmount, BigDecimal maxAmount) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    if ((from != null && (from.getYear() < 1 || from.getYear() > 9999))
        || (to != null && (to.getYear() < 1 || to.getYear() > 9999))) {
      throw new ValidationException("Dates must use years between 0001 and 9999");
    }
    if (from != null && to != null && from.isAfter(to)) {
      throw new ValidationException("from must not be after to");
    }
    if (type != null && !type.equals("CREDIT") && !type.equals("DEBIT")) {
      throw new ValidationException("type must be CREDIT or DEBIT");
    }
    if ((minAmount != null && minAmount.signum() < 0) || (maxAmount != null && maxAmount.signum() < 0)) {
      throw new ValidationException("Amount bounds must not be negative");
    }
    if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
      throw new ValidationException("minAmount must not exceed maxAmount");
    }
    LocalDateTime fromInclusive = from == null ? null : from.atStartOfDay();
    // MySQL cannot store a date beyond 9999-12-31, so no upper bound is needed for that day.
    LocalDateTime toExclusive = to == null || to.equals(LocalDate.of(9999, 12, 31))
        ? null : to.plusDays(1).atStartOfDay();
    List<TransactionEntity> activity = from == null && to == null && type == null
        && minAmount == null && maxAmount == null
        ? transactionRepository.findByAccountIdOrderByTransactionDateDescIdDesc(accountId)
        : transactionRepository.findActivity(accountId, fromInclusive, toExclusive, type, minAmount, maxAmount);
    return activity.stream()
        .map(transactionMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public TransactionResponse getActivityDetail(Long accountId, Long transferId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    TransactionEntity transaction = transactionRepository.findByIdAndAccountId(transferId, accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Transaction not found in this account"));
    return transactionMapper.toResponse(transaction);
  }

  @Transactional(readOnly = true)
  public List<TransactionResponse> getTransactions(Long accountId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    return transactionRepository.findTop5ByAccountIdOrderByTransactionDateDescIdDesc(accountId).stream()
        .map(transactionMapper::toResponse)
        .toList();
  }
}
