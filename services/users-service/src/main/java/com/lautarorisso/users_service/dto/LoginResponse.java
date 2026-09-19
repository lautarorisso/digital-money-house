package com.lautarorisso.users_service.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    int expiresIn
) {}