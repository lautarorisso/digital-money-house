package com.lautarorisso.users_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(max = 30, message = "Name must not exceed 30 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Name must not be blank")
    String nombre,

    @Size(max = 30, message = "Last name must not exceed 30 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Last name must not be blank")
    String apellido,

    @Positive(message = "DNI must be a positive number")
    @Max(value = 99999999, message = "DNI must not exceed 8 digits")
    Long dni,

    @Email(message = "Email format is invalid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Email must not be blank")
    String email,

    @Pattern(regexp = "^\\+?[0-9\\s()-]{6,20}$", message = "Phone format is invalid")
    String telefono,

    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Password must not be blank")
    String password
) {
}
