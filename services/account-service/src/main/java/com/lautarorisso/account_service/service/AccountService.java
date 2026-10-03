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
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AccountService {

  private static final String INTERNAL_CLIENT_ID = "dmh-backend";

  private final AccountRepository accountRepository;
  private final CvuGenerator cvuGenerator;
  private final AliasGenerator aliasGenerator;

  @Transactional
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
    return createForUser(userId, ownerSub);
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
    if (accountRepository.existsByUserId(userId)) {
      throw new ValidationException("An account already exists for this user");
    }
    AccountEntity account = accountRepository.save(
        new AccountEntity(userId, cvuGenerator.generate(), aliasGenerator.generate(), ownerSub));
    return new AccountResponse(account.getId(), account.getUserId(), account.getCvu(), account.getAlias());
  }
}
