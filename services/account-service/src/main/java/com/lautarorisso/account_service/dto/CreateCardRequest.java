package com.lautarorisso.account_service.dto;

import com.lautarorisso.account_service.entity.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCardRequest(
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "[0-9]{12,19}", message = "Card number must contain 12 to 19 digits")
    String cardNumber,
    @NotNull(message = "Card type is required")
    CardType type) {
}
