package com.lautarorisso.users_service.dto;

public record UserProfileResponse(
    Long id,
    String nombre,
    String apellido,
    Long dni,
    String email,
    String telefono
) {}
