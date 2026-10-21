package com.lautarorisso.account_service.service;

import com.lautarorisso.account_service.dto.AccountResponse;
import com.lautarorisso.account_service.dto.AccountDetailResponse;
import com.lautarorisso.account_service.entity.AccountEntity;
import com.lautarorisso.account_service.exception.ForbiddenException;
import com.lautarorisso.account_service.exception.ResourceNotFoundException;
import com.lautarorisso.account_service.exception.ValidationException;
import com.lautarorisso.account_service.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
@Service
public class AccountService {

  private static final String INTERNAL_CLIENT_ID = "dmh-backend";

  private final AccountRepository accountRepository;
  private final CvuGenerator cvuGenerator;
  private final AliasGenerator aliasGenerator;
  private final PlatformTransactionManager transactionManager;

  public AccountResponse createAccount(String azp, Long userId, String ownerSub) {
    if (!INTERNAL_CLIENT_ID.equals(azp)) {
      throw new ForbiddenException("Only the internal users-service can create accounts");
    }
    if (userId == null) {
      throw new ValidationException("User ID is required");
    }
    if (ownerSub == null || ownerSub.isBlank()) {
      throw new ValidationException("Owner subject is required");
    }
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    try {
      return transaction.execute(status -> createForUser(userId, ownerSub));
    } catch (DataIntegrityViolationException ex) {
      AccountEntity existing = accountRepository.findByUserId(userId).orElseThrow(() -> ex);
      if (!ownerSub.equals(existing.getOwnerSub())) {
        throw new ForbiddenException("The account belongs to a different identity");
      }
      return new AccountResponse(existing.getId(), existing.getUserId(), existing.getCvu(), existing.getAlias());
    }
  }

  @Transactional(readOnly = true)
  public AccountDetailResponse getAccountDetail(Long accountId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    return new AccountDetailResponse(account.getId(), account.getCvu(), account.getAlias(), account.getBalance());
  }

  private AccountResponse createForUser(Long userId, String ownerSub) {
    AccountEntity existing = accountRepository.findByUserId(userId).orElse(null);
    if (existing != null) {
      if (!ownerSub.equals(existing.getOwnerSub())) {
        throw new ForbiddenException("The account belongs to a different identity");
      }
      return new AccountResponse(existing.getId(), existing.getUserId(), existing.getCvu(), existing.getAlias());
    }
    AccountEntity account = accountRepository.saveAndFlush(
        new AccountEntity(userId, cvuGenerator.generate(), aliasGenerator.generate(), ownerSub));
    return new AccountResponse(account.getId(), account.getUserId(), account.getCvu(), account.getAlias());
  }

  @Transactional
  public AccountDetailResponse updateAlias(Long accountId, String subject, String alias) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    account.updateAlias(alias);
    try {
      accountRepository.saveAndFlush(account);
    } catch (DataIntegrityViolationException ex) {
      throw new ValidationException("Alias is already registered");
    }
    return new AccountDetailResponse(account.getId(), account.getCvu(), account.getAlias(), account.getBalance());
  }
}
