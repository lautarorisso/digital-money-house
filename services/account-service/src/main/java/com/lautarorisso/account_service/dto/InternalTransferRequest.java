package com.lautarorisso.account_service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record InternalTransferRequest(
    @NotBlank(message = "Destination is required") String destination,
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer and 2 decimal digits")
    BigDecimal amount) {}
