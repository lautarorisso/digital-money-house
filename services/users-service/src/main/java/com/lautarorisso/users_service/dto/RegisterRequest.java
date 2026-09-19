package com.lautarorisso.users_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name must not exceed 30 characters")
    String nombre,

    @NotBlank(message = "Last name is required")
    @Size(max = 30, message = "Last name must not exceed 30 characters")
    String apellido,

    @NotNull(message = "DNI is required")
    @Positive(message = "DNI must be a positive number")
    @Max(value = 99999999, message = "DNI must not exceed 8 digits")
    Long dni,

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    String email,

    @Pattern(regexp = "^\\+?[0-9\\s()-]{6,20}$", message = "Phone format is invalid")
    String telefono,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    String password
) {}