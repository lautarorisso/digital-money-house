package com.lautarorisso.account_service.dto;

import java.math.BigDecimal;

public record AccountDetailResponse(
    Long id,
    String cvu,
    String alias,
    BigDecimal balance
) {}
