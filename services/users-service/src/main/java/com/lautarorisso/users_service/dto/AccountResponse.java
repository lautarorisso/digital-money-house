package com.lautarorisso.users_service.dto;

public record AccountResponse(
    Long id,
    Long userId,
    String cvu,
    String alias
) {}