package com.lautarorisso.account_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
    @NotBlank(message = "Alias is required")
    @Size(max = 60, message = "Alias must not exceed 60 characters")
    String alias) {
}
