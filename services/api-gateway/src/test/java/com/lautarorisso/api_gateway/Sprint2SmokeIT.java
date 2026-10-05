package com.lautarorisso.api_gateway;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class Sprint2SmokeIT {

  private RequestSpecification api;
  private String email;
  private String accessToken;
  private Long userId;
  private Long accountId;
  private String cvu;
  private String alias;
  private String creditNumber;
  private String debitNumber;
  private Long creditCardId;
  private Long debitCardId;

  @BeforeAll
  void registerAndLogin() {
    api = new RequestSpecBuilder()
        .setBaseUri(System.getProperty("dmh.baseUrl", "http://localhost:8080"))
        .setContentType(ContentType.JSON)
        .setConfig(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
            .setParam("http.connection.timeout", 10000)
            .setParam("http.socket.timeout", 30000)))
        .build();
    email = "smoke-" + UUID.randomUUID() + "@dmh.test";
    String password = "Passw0rd!23";
    String numberPrefix = Long.toString(System.currentTimeMillis());
    creditNumber = numberPrefix + "001";
    debitNumber = numberPrefix + "002";

    Response registered = given().spec(api)
        .body(Map.of("nombre", "Juan", "apellido", "Perez", "dni", 30111223,
            "email", email, "telefono", "+541155551234", "password", password))
        .post("/users-service/users/register")
        .then().statusCode(201).extract().response();
    userId = registered.jsonPath().getLong("id");
    cvu = registered.jsonPath().getString("cvu");
    alias = registered.jsonPath().getString("alias");
    accessToken = given().spec(api)
        .body(Map.of("email", email, "password", password))
        .post("/users-service/auth/login")
        .then().statusCode(200).extract().path("accessToken");
    assertTrue(accessToken != null && !accessToken.isBlank(), "Login must return an access token");

    // Registration does not expose accountId; verify this candidate before using it.
    Response account = request().get("/accounts-service/accounts/{id}", userId)
        .then().statusCode(200).body("cvu", equalTo(cvu), "alias", equalTo(alias))
        .extract().response();
    accountId = account.jsonPath().getLong("id");
  }

  @Test
  @Order(15)
  @DisplayName("Case 15 - Account balance, CVU and alias")
  void case15AccountDetail() {
    Response account = request().get("/accounts-service/accounts/{id}", accountId)
        .then().statusCode(200).contentType(ContentType.JSON)
        .body("cvu", equalTo(cvu), "alias", equalTo(alias), "balance", equalTo(0.0f))
        .body("cvu", matchesPattern("[0-9]{22}"))
        .extract().response();
    assertEquals(accountId, account.jsonPath().getLong("id"));
  }

  @Test
  @Order(16)
  @DisplayName("Case 16 - Account without transactions")
  void case16EmptyTransactions() {
    request().get("/accounts-service/accounts/{id}/transactions", accountId)
        .then().statusCode(200).contentType(ContentType.JSON).body("", empty());
  }

  @Test
  @Order(17)
  @DisplayName("Case 17 - Own user profile")
  void case17UserProfile() {
    Response profile = request().get("/users-service/users/{id}", userId)
        .then().statusCode(200).contentType(ContentType.JSON)
        .body("nombre", equalTo("Juan"), "apellido", equalTo("Perez"),
            "dni", equalTo(30111223), "email", equalTo(email), "telefono", equalTo("+541155551234"))
        .body("keySet()", hasItems("id", "nombre", "apellido", "dni", "email", "telefono"))
        .body("size()", equalTo(6))
        .extract().response();
    assertEquals(userId, profile.jsonPath().getLong("id"));
  }

  @Test
  @Order(18)
  @DisplayName("Case 18 - Account without cards")
  void case18EmptyCards() {
    request().get("/cards/accounts/{id}/cards", accountId)
        .then().statusCode(200).contentType(ContentType.JSON).body("", empty());
  }

  @Test
  @Order(19)
  @DisplayName("Case 19 - Create a credit card")
  void case19CreateCreditCard() {
    Response card = request().body(Map.of("cardNumber", creditNumber, "type", "credit"))
        .post("/cards").then().statusCode(201).extract().response();
    creditCardId = card.jsonPath().getLong("id");
    assertTrue(creditCardId > 0);
    card.then().body("type", equalTo("CREDIT"))
        .body("keySet()", hasItems("id", "type"), "size()", equalTo(2));
  }

  @Test
  @Order(20)
  @DisplayName("Case 20 - Associate an existing card")
  void case20AssociateCreditCard() {
    Response card = request().body(Map.of("cardNumber", creditNumber, "type", "CREDIT"))
        .post("/accounts/{id}/cards", accountId)
        .then().statusCode(201).body("type", equalTo("CREDIT"))
        .body("keySet()", hasItems("id", "type"), "size()", equalTo(2))
        .extract().response();
    assertEquals(creditCardId, card.jsonPath().getLong("id"));
  }

  @Test
  @Order(21)
  @DisplayName("Case 21 - Create and associate a debit card")
  void case21CreateDebitCard() {
    Response card = request().body(Map.of("cardNumber", debitNumber, "type", "dEbIt"))
        .post("/accounts/{id}/cards", accountId)
        .then().statusCode(201).extract().response();
    debitCardId = card.jsonPath().getLong("id");
    assertTrue(debitCardId > 0);
    card.then().body("type", equalTo("DEBIT"))
        .body("keySet()", hasItems("id", "type"), "size()", equalTo(2));
  }

  @Test
  @Order(22)
  @DisplayName("Case 22 - List associated cards")
  void case22AssociatedCards() {
    Response cards = request().get("/cards/accounts/{id}/cards", accountId)
        .then().statusCode(200).contentType(ContentType.JSON)
        .body("size()", equalTo(2), "type", contains("CREDIT", "DEBIT"))
        .body("[0].keySet()", hasItems("id", "type"), "[0].size()", equalTo(2))
        .body("[1].keySet()", hasItems("id", "type"), "[1].size()", equalTo(2))
        .extract().response();
    assertEquals(List.of(creditCardId, debitCardId), cards.jsonPath().getList("id", Long.class));
  }

  @Test
  @Order(23)
  @DisplayName("Case 23 - Own card details")
  void case23CardDetail() {
    Response card = request().get("/cards/accounts/{accountId}/cards/{cardId}", accountId, creditCardId)
        .then().statusCode(200).contentType(ContentType.JSON)
        .body("type", equalTo("CREDIT"))
        .body("keySet()", hasItems("id", "type"), "size()", equalTo(2))
        .extract().response();
    assertEquals(creditCardId, card.jsonPath().getLong("id"));
  }

  @Test
  @Order(28)
  @DisplayName("Case 28 - Delete debit card")
  void case28DeleteDebitCard() {
    request().delete("/cards/accounts/{accountId}/cards/{cardId}", accountId, debitCardId)
        .then().statusCode(200).body(equalTo(""));
    debitCardId = null;
  }

  @Test
  @Order(29)
  @DisplayName("Case 29 - Delete credit card")
  void case29DeleteCreditCard() {
    request().delete("/cards/accounts/{accountId}/cards/{cardId}", accountId, creditCardId)
        .then().statusCode(200).body(equalTo(""));
    creditCardId = null;
    request().get("/cards/accounts/{id}/cards", accountId)
        .then().statusCode(200).body("", empty());
  }

  @AfterAll
  void cleanUpCards() {
    if (accountId == null || accessToken == null) return;
    for (Long id : new Long[] {creditCardId, debitCardId}) {
      if (id != null) {
        int status = request().delete("/cards/accounts/{accountId}/cards/{cardId}", accountId, id)
            .statusCode();
        assertTrue(status == 200 || status == 404, "Card cleanup returned HTTP " + status);
      }
    }
  }

  private RequestSpecification request() {
    return given().spec(api).auth().oauth2(accessToken);
  }
}
