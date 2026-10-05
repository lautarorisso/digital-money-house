package com.lautarorisso.account_service.service;

import com.lautarorisso.account_service.dto.CardResponse;
import com.lautarorisso.account_service.dto.CreateCardRequest;
import com.lautarorisso.account_service.entity.AccountEntity;
import com.lautarorisso.account_service.entity.CardEntity;
import com.lautarorisso.account_service.exception.ConflictException;
import com.lautarorisso.account_service.exception.ForbiddenException;
import com.lautarorisso.account_service.exception.ResourceNotFoundException;
import com.lautarorisso.account_service.mapper.CardMapper;
import com.lautarorisso.account_service.repository.AccountRepository;
import com.lautarorisso.account_service.repository.CardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CardService {

  private final CardRepository cardRepository;
  private final AccountRepository accountRepository;
  private final CardMapper cardMapper;

  @Transactional(readOnly = true)
  public List<CardResponse> getCardsByAccount(Long accountId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    return cardRepository.findByAccountIdOrderByIdAsc(accountId).stream()
        .map(cardMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public CardResponse getCardByAccount(Long accountId, Long cardId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    CardEntity card = cardRepository.findByIdAndAccountId(cardId, accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Card not found in this account"));
    return cardMapper.toResponse(card);
  }

  @Transactional
  public void deleteCard(Long accountId, Long cardId, String subject) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }
    CardEntity card = cardRepository.findByIdAndAccountId(cardId, accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Card not found in this account"));
    cardRepository.delete(card);
  }

  @Transactional
  public CardResponse createCard(CreateCardRequest request) {
    try {
      CardEntity card = cardRepository.saveAndFlush(new CardEntity(request.cardNumber(), request.type()));
      return cardMapper.toResponse(card);
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException("A card with this number already exists");
    }
  }

  @Transactional
  public CardResponse associateCard(Long accountId, String subject, CreateCardRequest request) {
    AccountEntity account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    if (!subject.equals(account.getOwnerSub())) {
      throw new ForbiddenException("You do not have access to this account");
    }

    CardEntity card = cardRepository.findByCardNumber(request.cardNumber()).orElse(null);
    if (card != null) {
      if (card.getAccountId() != null) {
        throw new ConflictException("Card is already associated with an account");
      }
      if (cardRepository.associateIfUnassociated(card.getId(), accountId) == 0) {
        throw new ConflictException("Card is already associated with an account");
      }
      return cardMapper.toResponse(card);
    }

    card = new CardEntity(request.cardNumber(), request.type());
    card.associateWith(accountId);
    try {
      return cardMapper.toResponse(cardRepository.saveAndFlush(card));
    } catch (DataIntegrityViolationException exception) {
      throw new ConflictException("A card with this number already exists");
    }
  }
}
