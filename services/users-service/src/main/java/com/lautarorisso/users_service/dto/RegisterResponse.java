package com.lautarorisso.users_service.dto;

public record RegisterResponse(
    Long id,
    String nombre,
    String apellido,
    Long dni,
    String email,
    String telefono,
    String cvu,
    String alias
) {}