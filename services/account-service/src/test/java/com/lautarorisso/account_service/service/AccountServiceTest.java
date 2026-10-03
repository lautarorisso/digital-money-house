package com.lautarorisso.account_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lautarorisso.account_service.dto.AccountResponse;
import com.lautarorisso.account_service.entity.AccountEntity;
import com.lautarorisso.account_service.exception.ValidationException;
import com.lautarorisso.account_service.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock AccountRepository accountRepository;
  @Mock CvuGenerator cvuGenerator;
  @Mock AliasGenerator aliasGenerator;
  @Mock AccountEntity savedAccount;
  private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService = new AccountService(accountRepository, cvuGenerator, aliasGenerator);
  }

  @Test
  void createAccountPersistsGeneratedCvuAndAlias() {
    when(cvuGenerator.generate()).thenReturn("1234567890123456789012");
    when(aliasGenerator.generate()).thenReturn("casa.rio.sol");
    when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedAccount);
    when(savedAccount.getId()).thenReturn(9L);
    when(savedAccount.getUserId()).thenReturn(42L);
    when(savedAccount.getCvu()).thenReturn("1234567890123456789012");
    when(savedAccount.getAlias()).thenReturn("casa.rio.sol");

    AccountResponse response = accountService.createAccount("dmh-backend", 42L, "subject-123");

    assertEquals("1234567890123456789012", response.cvu());
    assertEquals("casa.rio.sol", response.alias());
    ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
    verify(accountRepository).save(captor.capture());
    assertEquals(42L, captor.getValue().getUserId());
    assertEquals("subject-123", captor.getValue().getOwnerSub());
  }

  @Test
  void createAccountRejectsDuplicateWithoutGeneratingOrSaving() {
    when(accountRepository.existsByUserId(42L)).thenReturn(true);

    assertThrows(ValidationException.class,
        () -> accountService.createAccount("dmh-backend", 42L, "subject-123"));

    verify(cvuGenerator, never()).generate();
    verify(aliasGenerator, never()).generate();
    verify(accountRepository, never()).save(any());
  }
}
