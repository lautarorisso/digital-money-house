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
  public List<TransactionResponse> getActivity(Long accountId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    return transactionRepository.findByAccountIdOrderByTransactionDateDescIdDesc(accountId).stream()
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
