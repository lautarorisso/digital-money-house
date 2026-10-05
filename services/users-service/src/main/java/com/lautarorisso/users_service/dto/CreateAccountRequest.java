package com.lautarorisso.users_service.dto;

import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
    @NotNull(message = "User ID is required")
    Long userId,
    @NotNull(message = "Owner subject is required")
    String ownerSub
) {}
