package com.lautarorisso.account_service.mapper;

import com.lautarorisso.account_service.dto.CardResponse;
import com.lautarorisso.account_service.entity.CardEntity;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

  public CardResponse toResponse(CardEntity card) {
    return new CardResponse(card.getId(), card.getType());
  }
}
