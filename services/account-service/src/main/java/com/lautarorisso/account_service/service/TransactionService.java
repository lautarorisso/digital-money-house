package com.lautarorisso.account_service.service;

import com.lautarorisso.account_service.dto.TransactionResponse;
import com.lautarorisso.account_service.entity.AccountEntity;
import com.lautarorisso.account_service.entity.TransactionEntity;
import com.lautarorisso.account_service.exception.ForbiddenException;
import com.lautarorisso.account_service.exception.ResourceNotFoundException;
import com.lautarorisso.account_service.repository.AccountRepository;
import com.lautarorisso.account_service.repository.TransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class TransactionService {

  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;

  @Transactional(readOnly = true)
  public List<TransactionResponse> getTransactions(Long accountId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    return transactionRepository.findTop5ByAccountIdOrderByTransactionDateDescIdDesc(accountId).stream()
        .map(this::toResponse)
        .toList();
  }

  private TransactionResponse toResponse(TransactionEntity transaction) {
    return new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getType(),
        transaction.getDescription(), transaction.getTransactionDate(), transaction.getBalanceAfter());
  }
}
