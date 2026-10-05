package com.lautarorisso.account_service.dto;

import com.lautarorisso.account_service.entity.CardType;

public record CardResponse(Long id, CardType type) {
}
