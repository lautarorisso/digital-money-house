package com.lautarorisso.users_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.lautarorisso.users_service.dto.LoginResponse;
import com.lautarorisso.users_service.exception.ServiceUnavailableException;
import com.lautarorisso.users_service.exception.ValidationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class KeycloakClient {

  private static final Logger log = LoggerFactory.getLogger(KeycloakClient.class);

  private final RestClient restClient;
  private final String baseUrl;
  private final String realm;
  private final String backendClientId;
  private final String backendClientSecret;
  private final String frontendClientId;

  public KeycloakClient(RestClient.Builder restClientBuilder,
      @Value("${keycloak.base-url}") String baseUrl,
      @Value("${keycloak.realm}") String realm,
      @Value("${keycloak.backend-client-id}") String backendClientId,
      @Value("${keycloak.backend-client-secret}") String backendClientSecret,
      @Value("${keycloak.frontend-client-id}") String frontendClientId) {
    this.restClient = restClientBuilder.build();
    this.baseUrl = baseUrl;
    this.realm = realm;
    this.backendClientId = backendClientId;
    this.backendClientSecret = backendClientSecret;
    this.frontendClientId = frontendClientId;
  }

  public String getServiceAccountToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", backendClientId);
    form.add("client_secret", backendClientSecret);
    form.add("grant_type", "client_credentials");
    JsonNode response;
    try {
      response = postToken(form);
    } catch (HttpClientErrorException e) {
      throw new ServiceUnavailableException(
          "Identity provider rejected the service account credentials (" + e.getStatusCode().value() + ")", e);
    }
    if (response.get("access_token") == null) {
      throw new IllegalStateException("Keycloak token response did not contain an access token");
    }
    return response.get("access_token").asText();
  }

  public String createUser(String token, String username, String email, String firstName, String lastName,
      String password) {
    KeycloakUserRequest payload = new KeycloakUserRequest(username, email, true, firstName, lastName,
        List.of(new KeycloakUserRequest.Credential("password", password, false)));
    try {
      ResponseEntity<Void> response = restClient.post()
          .uri(baseUrl + "/admin/realms/" + realm + "/users")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
      if (response.getHeaders().getLocation() == null) {
        throw new IllegalStateException("Keycloak did not return the location of the created user");
      }
      String location = response.getHeaders().getLocation().toString();
      return location.substring(location.lastIndexOf('/') + 1);
    } catch (HttpClientErrorException.Conflict e) {
      throw new ValidationException("Email is already registered in the identity provider");
    } catch (HttpClientErrorException.BadRequest e) {
      throw new ValidationException(translateBadRequest(e));
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      throw new ServiceUnavailableException(
          "Identity provider failed during user creation (" + e.getStatusCode().value() + ")", e);
    }
  }

  private String translateBadRequest(HttpClientErrorException.BadRequest ex) {
    try {
      JsonNode body = ex.getResponseBodyAs(JsonNode.class);
      if (body != null) {
        String errorMessage = body.path("errorMessage").asText("");
        if (errorMessage.contains("person-name-invalid-character")) {
          return "lastName".equals(body.path("field").asText())
              ? "Last name contains invalid characters"
              : "Name contains invalid characters";
        }
        if (errorMessage.contains("username-invalid-character")) {
          return "Email contains invalid characters";
        }
      }
    } catch (Exception ignored) {
      log.debug("Could not read the error body returned by Keycloak", ignored);
    }
    return "Invalid registration data";
  }

  public void deleteUser(String token, String userId) {
    try {
      restClient.delete()
          .uri(baseUrl + "/admin/realms/" + realm + "/users/" + userId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException | HttpServerErrorException | ResourceAccessException e) {
      throw new ServiceUnavailableException("Identity provider failed during user deletion", e);
    }
  }

  public LoginResponse login(String email, String password) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", frontendClientId);
    form.add("grant_type", "password");
    form.add("username", email);
    form.add("password", password);
    JsonNode response;
    try {
      response = postToken(form);
    } catch (HttpClientErrorException.BadRequest e) {
      throw new ValidationException("Invalid credentials");
    } catch (HttpClientErrorException e) {
      throw new ServiceUnavailableException(
          "Identity provider failed during login (" + e.getStatusCode().value() + ")", e);
    }
    if (response.get("access_token") == null) {
      throw new IllegalStateException("Keycloak token response did not contain an access token");
    }
    return new LoginResponse(
        response.get("access_token").asText(),
        response.path("refresh_token").asText(null),
        response.path("token_type").asText("bearer"),
        response.path("expires_in").asInt(300));
  }

  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", frontendClientId);
    form.add("refresh_token", refreshToken);
    try {
      restClient.post()
          .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/logout")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException e) {
      log.info("Logout: refresh token already invalid or revoked in Keycloak ({}), session considered closed",
          e.getStatusCode().value());
    } catch (HttpServerErrorException | ResourceAccessException e) {
      throw new ServiceUnavailableException("Identity provider unavailable during logout", e);
    }
  }

  private JsonNode postToken(MultiValueMap<String, String> form) {
    try {
      return restClient.post()
          .uri(baseUrl + "/realms/" + realm + "/protocol/openid-connect/token")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(JsonNode.class);
    } catch (HttpServerErrorException | ResourceAccessException e) {
      throw new ServiceUnavailableException("Identity provider unavailable", e);
    }
  }
}