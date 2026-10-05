package com.lautarorisso.account_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    BigDecimal amount,
    String type,
    String description,
    LocalDateTime transactionDate,
    BigDecimal balanceAfter) {
}
