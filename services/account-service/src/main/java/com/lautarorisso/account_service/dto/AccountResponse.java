package com.lautarorisso.account_service.dto;

public record AccountResponse(
    Long id,
    Long userId,
    String cvu,
    String alias
) {}