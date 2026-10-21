package com.lautarorisso.account_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lautarorisso.account_service.dto.CreateTransferenceRequest;
import com.lautarorisso.account_service.entity.*;
import com.lautarorisso.account_service.exception.*;
import com.lautarorisso.account_service.mapper.TransactionMapper;
import com.lautarorisso.account_service.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
  @Mock AccountRepository accounts;
  @Mock TransactionRepository transactions;
  @Mock CardRepository cards;
  private TransactionService service;
  private final AccountEntity account = new AccountEntity(1L, "123", "casa.rio.sol", "owner");
  private final TransactionMapper mapper = new TransactionMapper();
  private final TransactionEntity transaction = new TransactionEntity(1L, BigDecimal.TEN,
      "CREDIT", "Card deposit", LocalDateTime.of(2026, 1, 1, 12, 0), BigDecimal.TEN);

  @BeforeEach
  void setUp() {
    service = new TransactionService(accounts, transactions, mapper, cards);
  }

  @Test
  void unfilteredActivityPreservesRepositoryOrderAndMapping() {
    when(accounts.findById(1L)).thenReturn(Optional.of(account));
    TransactionEntity second = new TransactionEntity(1L, BigDecimal.ONE, "DEBIT", "Other",
        LocalDateTime.of(2025, 1, 1, 12, 0), BigDecimal.ZERO);
    when(transactions.findByAccountIdOrderByTransactionDateDescIdDesc(1L))
        .thenReturn(List.of(transaction, second));
    assertEquals(List.of(mapper.toResponse(transaction), mapper.toResponse(second)),
        service.getActivity(1L, "owner", null, null, null, null, null));
    verifyNoMoreInteractions(transactions);
  }

  @Test
  void combinedFiltersConvertInclusiveDatesAndForwardAmountBounds() {
    when(accounts.findById(1L)).thenReturn(Optional.of(account));
    LocalDate day = LocalDate.of(2026, 1, 1);
    when(transactions.findActivity(1L, day.atStartOfDay(), day.plusDays(1).atStartOfDay(),
        "CREDIT", BigDecimal.ZERO, BigDecimal.TEN)).thenReturn(List.of(transaction));
    assertEquals(List.of(mapper.toResponse(transaction)), service.getActivity(1L, "owner",
        day, day, "CREDIT", BigDecimal.ZERO, BigDecimal.TEN));
    verifyNoMoreInteractions(transactions);
  }

  @Test
  void optionalAndMaximumDateBoundsUseFilteredPath() {
    when(accounts.findById(1L)).thenReturn(Optional.of(account));
    LocalDate first = LocalDate.of(1, 1, 1);
    assertEquals(List.of(), service.getActivity(1L, "owner", first, null, null, null, null));
    verify(transactions).findActivity(1L, first.atStartOfDay(), null, null, null, null);
    assertEquals(List.of(), service.getActivity(1L, "owner", null,
        LocalDate.of(9999, 12, 31), "DEBIT", null, null));
    verify(transactions).findActivity(1L, null, null, "DEBIT", null, null);
    LocalDate day = LocalDate.of(2026, 1, 1);
    assertEquals(List.of(), service.getActivity(1L, "owner", null, day, null, null, null));
    verify(transactions).findActivity(1L, null, day.plusDays(1).atStartOfDay(), null, null, null);
    assertEquals(List.of(), service.getActivity(1L, "owner", null, null, null,
        BigDecimal.TEN, BigDecimal.TEN));
    verify(transactions).findActivity(1L, null, null, null, BigDecimal.TEN, BigDecimal.TEN);
    verifyNoMoreInteractions(transactions);
  }

  static Stream<Arguments> invalidFilters() {
    return Stream.of(
        Arguments.of(LocalDate.of(0, 1, 1), null, null, null, null, "Dates must use years between 0001 and 9999"),
        Arguments.of(null, LocalDate.of(10000, 1, 1), null, null, null, "Dates must use years between 0001 and 9999"),
        Arguments.of(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, null, null, "from must not be after to"),
        Arguments.of(null, null, "credit", null, null, "type must be CREDIT or DEBIT"),
        Arguments.of(null, null, "UNKNOWN", null, null, "type must be CREDIT or DEBIT"),
        Arguments.of(null, null, null, BigDecimal.ONE.negate(), null, "Amount bounds must not be negative"),
        Arguments.of(null, null, null, null, BigDecimal.ONE.negate(), "Amount bounds must not be negative"),
        Arguments.of(null, null, null, BigDecimal.TEN, BigDecimal.ONE, "minAmount must not exceed maxAmount"));
  }

  @ParameterizedTest
  @MethodSource("invalidFilters")
  void invalidFiltersNeverQueryOrMutate(LocalDate from, LocalDate to, String type,
      BigDecimal min, BigDecimal max, String message) {
    when(accounts.findById(1L)).thenReturn(Optional.of(account));
    assertEquals(message, assertThrows(ValidationException.class,
        () -> service.getActivity(1L, "owner", from, to, type, min, max)).getMessage());
    assertEquals(BigDecimal.ZERO, account.getBalance());
    verifyNoInteractions(transactions, cards);
    verify(accounts).findById(1L);
    verifyNoMoreInteractions(accounts);
  }

  @ParameterizedTest
  @EnumSource(CardType.class)
  void cardDepositCreditsBalanceAndPersistsCompleteTransaction(CardType type) {
    account.credit(BigDecimal.ONE);
    when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
    when(cards.findByIdAndAccountId(2L, 1L))
        .thenReturn(Optional.of(new CardEntity("1234567890123456", type)));
    when(transactions.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    LocalDateTime before = LocalDateTime.now();
    var response = service.createTransference(1L, "owner", new CreateTransferenceRequest(2L, BigDecimal.TEN));
    assertEquals(new BigDecimal("11"), account.getBalance());
    var captor = org.mockito.ArgumentCaptor.forClass(TransactionEntity.class);
    verify(transactions).save(captor.capture());
    var saved = captor.getValue();
    assertEquals(1L, saved.getAccountId());
    assertEquals(BigDecimal.TEN, saved.getAmount());
    assertEquals("CREDIT", saved.getType());
    assertEquals("Card deposit", saved.getDescription());
    assertEquals(account.getBalance(), saved.getBalanceAfter());
    assertFalse(saved.getTransactionDate().isBefore(before));
    assertFalse(saved.getTransactionDate().isAfter(LocalDateTime.now()));
    assertEquals(mapper.toResponse(saved), response);
    verifyNoMoreInteractions(transactions);
  }

  @Test
  void depositGuardsNeverChangeBalanceOrSave() {
    var request = new CreateTransferenceRequest(2L, BigDecimal.TEN);
    assertEquals("Account not found", assertThrows(ResourceNotFoundException.class,
        () -> service.createTransference(1L, "owner", request)).getMessage());
    when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
    assertEquals("You do not have access to this account", assertThrows(ForbiddenException.class,
        () -> service.createTransference(1L, "other", request)).getMessage());
    verifyNoInteractions(cards);
    assertEquals("Card not found in this account", assertThrows(ResourceNotFoundException.class,
        () -> service.createTransference(1L, "owner", request)).getMessage());
    assertEquals(BigDecimal.ZERO, account.getBalance());
    account.credit(new BigDecimal("9999999999.99"));
    when(cards.findByIdAndAccountId(2L, 1L)).thenReturn(Optional.of(new CardEntity()));
    assertEquals("Amount exceeds account balance limit", assertThrows(ValidationException.class,
        () -> service.createTransference(1L, "owner", request)).getMessage());
    assertEquals(new BigDecimal("9999999999.99"), account.getBalance());
    verifyNoInteractions(transactions);
    verify(accounts, never()).save(any());
  }

  @Test
  void detailUsesAccountScopedLookupAndRealMapper() {
    when(accounts.findById(1L)).thenReturn(Optional.of(account));
    when(transactions.findByIdAndAccountId(9L, 1L)).thenReturn(Optional.of(transaction));
    assertEquals(mapper.toResponse(transaction), service.getActivityDetail(1L, 9L, "owner"));
    verify(transactions).findByIdAndAccountId(9L, 1L);
    verifyNoMoreInteractions(transactions);
  }

  @Test
  void detailRejectsMissingAccountForeignOwnerAndMissingTransaction() {
    assertEquals("Account not found", assertThrows(ResourceNotFoundException.class,
        () -> service.getActivityDetail(1L, 9L, "owner")).getMessage());
    when(accounts.findById(1L)).thenReturn(Optional.of(account));
    assertEquals("You do not have access to this account", assertThrows(ForbiddenException.class,
        () -> service.getActivityDetail(1L, 9L, "other")).getMessage());
    verifyNoInteractions(transactions);
    assertEquals("Transaction not found in this account", assertThrows(ResourceNotFoundException.class,
        () -> service.getActivityDetail(1L, 9L, "owner")).getMessage());
    verify(transactions).findByIdAndAccountId(9L, 1L);
    verifyNoMoreInteractions(transactions);
    assertEquals(BigDecimal.ZERO, account.getBalance());
  }

  @Test
  void activityStillRequiresExistingAccountAndOwnership() {
    assertThrows(ResourceNotFoundException.class,
        () -> service.getActivity(1L, "owner", null, null, null, null, null));
    when(accounts.findById(1L)).thenReturn(Optional.of(account));
    assertThrows(ForbiddenException.class,
        () -> service.getActivity(1L, "other", null, null, "DEBIT", null, null));
    verifyNoInteractions(transactions, cards);
  }
}
