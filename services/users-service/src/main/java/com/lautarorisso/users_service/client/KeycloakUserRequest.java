package com.lautarorisso.users_service.client;

import java.util.List;

public record KeycloakUserRequest(
    String username,
    String email,
    boolean enabled,
    String firstName,
    String lastName,
    List<Credential> credentials
) {

  public record Credential(String type, String value, boolean temporary) {
  }
}