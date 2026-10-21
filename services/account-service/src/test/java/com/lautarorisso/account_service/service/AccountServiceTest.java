package com.lautarorisso.account_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lautarorisso.account_service.dto.AccountResponse;
import com.lautarorisso.account_service.entity.AccountEntity;
import com.lautarorisso.account_service.exception.ForbiddenException;
import com.lautarorisso.account_service.exception.ValidationException;
import com.lautarorisso.account_service.repository.AccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock AccountRepository accountRepository;
  @Mock CvuGenerator cvuGenerator;
  @Mock AliasGenerator aliasGenerator;
  @Mock AccountEntity savedAccount;
  @Mock PlatformTransactionManager transactionManager;
  private AccountService accountService;

  @BeforeEach
  void setUp() {
    accountService = new AccountService(accountRepository, cvuGenerator, aliasGenerator, transactionManager);
  }

  @Test
  void createAccountPersistsGeneratedCvuAndAlias() {
    when(cvuGenerator.generate()).thenReturn("1234567890123456789012");
    when(aliasGenerator.generate()).thenReturn("casa.rio.sol");
    when(accountRepository.saveAndFlush(any(AccountEntity.class))).thenReturn(savedAccount);
    when(savedAccount.getId()).thenReturn(9L);
    when(savedAccount.getUserId()).thenReturn(42L);
    when(savedAccount.getCvu()).thenReturn("1234567890123456789012");
    when(savedAccount.getAlias()).thenReturn("casa.rio.sol");

    AccountResponse response = accountService.createAccount("dmh-backend", 42L, "subject-123");

    assertEquals(new AccountResponse(9L, 42L, "1234567890123456789012", "casa.rio.sol"), response);
    ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
    verify(accountRepository).saveAndFlush(captor.capture());
    assertEquals(42L, captor.getValue().getUserId());
    assertEquals("subject-123", captor.getValue().getOwnerSub());
    verify(transactionManager).commit(null);
  }

  @Test
  void createAccountReturnsExistingWithoutGeneratingOrSaving() {
    when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(savedAccount));
    when(savedAccount.getOwnerSub()).thenReturn("subject-123");
    when(savedAccount.getId()).thenReturn(9L);
    when(savedAccount.getUserId()).thenReturn(42L);
    when(savedAccount.getCvu()).thenReturn("1234567890123456789012");
    when(savedAccount.getAlias()).thenReturn("casa.rio.sol");

    AccountResponse expected = new AccountResponse(9L, 42L, "1234567890123456789012", "casa.rio.sol");
    assertEquals(expected, accountService.createAccount("dmh-backend", 42L, "subject-123"));
    assertEquals(expected, accountService.createAccount("dmh-backend", 42L, "subject-123"));

    verifyNoInteractions(cvuGenerator, aliasGenerator);
    verify(accountRepository, never()).saveAndFlush(any());
  }

  @Test
  void createAccountRejectsAnotherIdentityWithoutWrites() {
    when(accountRepository.findByUserId(42L)).thenReturn(Optional.of(savedAccount));
    when(savedAccount.getOwnerSub()).thenReturn("other-owner");

    assertThrows(ForbiddenException.class,
        () -> accountService.createAccount("dmh-backend", 42L, "subject-123"));

    verifyNoInteractions(cvuGenerator, aliasGenerator);
    verify(accountRepository, never()).saveAndFlush(any());
    verify(transactionManager).rollback(null);
  }

  @Test
  void createAccountRejectsUnauthorizedClientAndMissingIdentityBeforeDatabaseAccess() {
    assertThrows(ForbiddenException.class,
        () -> accountService.createAccount("frontend", 42L, "subject-123"));
    assertThrows(ValidationException.class,
        () -> accountService.createAccount("dmh-backend", null, "subject-123"));
    assertThrows(ValidationException.class,
        () -> accountService.createAccount("dmh-backend", 42L, " "));
    verifyNoInteractions(accountRepository, cvuGenerator, aliasGenerator, transactionManager);
  }

  @Test
  void concurrentCreationReadsWinnerOnlyAfterRollback() {
    when(accountRepository.findByUserId(42L)).thenReturn(Optional.empty(), Optional.of(savedAccount));
    when(cvuGenerator.generate()).thenReturn("1234567890123456789012");
    when(aliasGenerator.generate()).thenReturn("casa.rio.sol");
    when(accountRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
    when(savedAccount.getOwnerSub()).thenReturn("subject-123");
    when(savedAccount.getId()).thenReturn(9L);
    when(savedAccount.getUserId()).thenReturn(42L);
    when(savedAccount.getCvu()).thenReturn("1234567890123456789012");
    when(savedAccount.getAlias()).thenReturn("casa.rio.sol");

    assertEquals(new AccountResponse(9L, 42L, "1234567890123456789012", "casa.rio.sol"),
        accountService.createAccount("dmh-backend", 42L, "subject-123"));

    var order = inOrder(accountRepository, transactionManager);
    order.verify(accountRepository).findByUserId(42L);
    order.verify(accountRepository).saveAndFlush(any());
    order.verify(transactionManager).rollback(null);
    order.verify(accountRepository).findByUserId(42L);
    verify(transactionManager, never()).commit(any());
  }

  @Test
  void concurrentCreationNeverReturnsAnotherIdentity() {
    when(accountRepository.findByUserId(42L)).thenReturn(Optional.empty(), Optional.of(savedAccount));
    when(cvuGenerator.generate()).thenReturn("1234567890123456789012");
    when(aliasGenerator.generate()).thenReturn("casa.rio.sol");
    when(accountRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
    when(savedAccount.getOwnerSub()).thenReturn("other-owner");

    assertThrows(ForbiddenException.class,
        () -> accountService.createAccount("dmh-backend", 42L, "subject-123"));
    verify(transactionManager).rollback(null);
    verify(savedAccount, never()).getCvu();
    verify(savedAccount, never()).getAlias();
  }

  @Test
  void unrelatedConstraintFailureIsNotReportedAsSuccess() {
    when(cvuGenerator.generate()).thenReturn("1234567890123456789012");
    when(aliasGenerator.generate()).thenReturn("casa.rio.sol");
    var failure = new DataIntegrityViolationException("alias collision");
    when(accountRepository.saveAndFlush(any())).thenThrow(failure);

    assertSame(failure, assertThrows(DataIntegrityViolationException.class,
        () -> accountService.createAccount("dmh-backend", 42L, "subject-123")));
    verify(transactionManager).rollback(null);
  }
}
