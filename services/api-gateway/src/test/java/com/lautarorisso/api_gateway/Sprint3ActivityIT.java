package com.lautarorisso.api_gateway;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
class Sprint3ActivityIT {

  private record TestAccount(Long id, String token) {
  }

  private RequestSpecification api;
  private TestAccount primary;
  private TestAccount secondary;
  private Long creditCardId;
  private Long debitCardId;
  private Long secondaryCardId;
  private Long creditTransferId;
  private BigDecimal expectedBalance = BigDecimal.ZERO;
  private final List<Long> transferIds = new ArrayList<>();

  @BeforeAll
  void prepareAccountsAndCards() {
    api = new RequestSpecBuilder()
        .setBaseUri(System.getProperty("dmh.baseUrl", "http://localhost:8080"))
        .setContentType(ContentType.JSON)
        .setConfig(RestAssuredConfig.config()
            .jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL))
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", 10000)
                .setParam("http.socket.timeout", 30000)))
        .build();
    primary = createAccount();
    secondary = createAccount();
    String prefix = Long.toString(System.currentTimeMillis());
    creditCardId = createCard(primary, prefix + "101", "CREDIT");
    debitCardId = createCard(primary, prefix + "102", "DEBIT");
    secondaryCardId = createCard(secondary, prefix + "103", "CREDIT");
  }

  @Test
  @Order(1)
  @DisplayName("Case 1 - Account without activity")
  void case1EmptyActivity() {
    request(primary).get("/accounts/{id}/activity", primary.id())
        .then().statusCode(200).contentType(ContentType.JSON).body("", empty());
  }

  @Test
  @Order(2)
  @DisplayName("Case 9 - Deposit from a credit card")
  void case9CreditCardDeposit() {
    creditTransferId = createDeposit(creditCardId, new BigDecimal("1000.50"));
    request(primary).get("/accounts-service/accounts/{id}", primary.id())
        .then().statusCode(200).body("balance", comparesEqualTo(new BigDecimal("1000.50")));
  }

  @Test
  @Order(3)
  @DisplayName("Case 10 - Deposit from a debit card")
  void case10DebitCardDeposit() {
    createDeposit(debitCardId, new BigDecimal("250.00"));
    request(primary).get("/accounts-service/accounts/{id}", primary.id())
        .then().statusCode(200).body("balance", comparesEqualTo(new BigDecimal("1250.50")));
  }

  @Test
  @Order(4)
  @DisplayName("Case 2 - Complete activity ordered newest first")
  void case2CompleteActivity() {
    for (int i = 0; i < 4; i++) {
      createDeposit(debitCardId, new BigDecimal("0.25"));
    }
    Response activity = request(primary).get("/accounts/{id}/activity", primary.id())
        .then().statusCode(200).contentType(ContentType.JSON)
        .body("size()", equalTo(6))
        .body("[0].balanceAfter", comparesEqualTo(new BigDecimal("1251.50")))
        .extract().response();
    List<Long> newestFirst = new ArrayList<>(transferIds);
    Collections.reverse(newestFirst);
    assertEquals(newestFirst, activity.jsonPath().getList("id", Long.class));
    List<String> dates = activity.jsonPath().getList("transactionDate", String.class);
    for (int i = 1; i < dates.size(); i++) {
      LocalDateTime previous = LocalDateTime.parse(dates.get(i - 1));
      LocalDateTime current = LocalDateTime.parse(dates.get(i));
      assertTrue(!current.isAfter(previous), "Activity must be ordered newest first");
      if (current.equals(previous)) {
        assertTrue(newestFirst.get(i - 1) > newestFirst.get(i), "Equal dates must use descending IDs");
      }
    }
  }

  @Test
  @Order(5)
  @DisplayName("Case 3 - Activity from another owner's account")
  void case3ForeignActivity() {
    request(primary).get("/accounts/{id}/activity", secondary.id())
        .then().statusCode(403).body("status", equalTo(403),
            "message", equalTo("You do not have access to this account"));
  }

  @Test
  @Order(6)
  @DisplayName("Case 4 - Invalid account ID")
  void case4InvalidAccountId() {
    request(primary).get("/accounts/{id}/activity", "invalid")
        .then().statusCode(400).body("status", equalTo(400), "message", equalTo("Invalid path parameter"));
  }

  @Test
  @Order(7)
  @DisplayName("Case 5 - Own activity details")
  void case5OwnActivityDetail() {
    Response detail = request(primary).get("/accounts/{accountId}/activity/{transferId}",
            primary.id(), creditTransferId)
        .then().statusCode(200).contentType(ContentType.JSON)
        .body("amount", comparesEqualTo(new BigDecimal("1000.50")), "type", equalTo("CREDIT"))
        .body("balanceAfter", comparesEqualTo(new BigDecimal("1000.50")))
        .body("keySet()", hasItems("id", "amount", "type", "description", "transactionDate", "balanceAfter"))
        .body("size()", equalTo(6))
        .extract().response();
    assertEquals(creditTransferId, detail.jsonPath().getLong("id"));
    assertTrue(!detail.jsonPath().getString("description").isBlank());
    LocalDateTime.parse(detail.jsonPath().getString("transactionDate"));
  }

  @Test
  @Order(8)
  @DisplayName("Case 6 - Missing transfer")
  void case6MissingTransfer() {
    request(primary).get("/accounts/{accountId}/activity/{transferId}", primary.id(), Long.MAX_VALUE)
        .then().statusCode(404).body("status", equalTo(404),
            "message", equalTo("Transaction not found in this account"));
  }

  @Test
  @Order(9)
  @DisplayName("Case 7 - Details from another owner's account")
  void case7ForeignActivityDetail() {
    request(primary).get("/accounts/{accountId}/activity/{transferId}", secondary.id(), creditTransferId)
        .then().statusCode(403).body("status", equalTo(403),
            "message", equalTo("You do not have access to this account"));
  }

  @Test
  @Order(10)
  @DisplayName("Case 8 - Transfer outside the authenticated account")
  void case8TransferOutsideOwnAccount() {
    request(secondary).get("/accounts/{accountId}/activity/{transferId}", secondary.id(), creditTransferId)
        .then().statusCode(404).body("status", equalTo(404),
            "message", equalTo("Transaction not found in this account"));
  }

  @Test
  @Order(11)
  @DisplayName("Case 11 - Zero and negative amounts")
  void case11NonPositiveAmount() {
    for (BigDecimal amount : List.of(BigDecimal.ZERO, new BigDecimal("-10"))) {
      request(primary).body(Map.of("cardId", creditCardId, "amount", amount))
          .post("/accounts/{id}/transferences", primary.id())
          .then().statusCode(400).body("status", equalTo(400),
              "message", equalTo("Amount must be at least 0.01"));
    }
    assertAccountsUnchanged();
  }

  @Test
  @Order(12)
  @DisplayName("Case 12 - Deposit into a missing account")
  void case12MissingAccount() {
    request(primary).body(Map.of("cardId", creditCardId, "amount", BigDecimal.TEN))
        .post("/accounts/{id}/transferences", Long.MAX_VALUE)
        .then().statusCode(404).body("status", equalTo(404), "message", equalTo("Account not found"));
    assertAccountsUnchanged();
  }

  @Test
  @Order(13)
  @DisplayName("Case 13 - Card not associated with the account")
  void case13CardOutsideAccount() {
    request(primary).body(Map.of("cardId", secondaryCardId, "amount", BigDecimal.TEN))
        .post("/accounts/{id}/transferences", primary.id())
        .then().statusCode(404).body("status", equalTo(404), "message", equalTo("Card not found in this account"));
    assertAccountsUnchanged();
  }

  @Test
  @Order(14)
  @DisplayName("Case 14 - Deposit into another owner's account")
  void case14ForeignAccountDeposit() {
    request(primary).body(Map.of("cardId", secondaryCardId, "amount", BigDecimal.TEN))
        .post("/accounts/{id}/transferences", secondary.id())
        .then().statusCode(403).body("status", equalTo(403),
            "message", equalTo("You do not have access to this account"));
    assertAccountsUnchanged();
  }

  @AfterAll
  void cleanUpCards() {
    deleteCard(primary, creditCardId);
    deleteCard(primary, debitCardId);
    deleteCard(secondary, secondaryCardId);
  }

  private TestAccount createAccount() {
    String email = "lautaro-sprint3-" + UUID.randomUUID() + "@dmh.test";
    String password = "Passw0rd!23";
    Response registered = given().spec(api)
        .body(Map.of("nombre", "Lautaro", "apellido", "Risso", "dni", 30111223,
            "email", email, "telefono", "+541155551234", "password", password))
        .post("/users-service/users/register")
        .then().statusCode(201).extract().response();
    String token = given().spec(api).body(Map.of("email", email, "password", password))
        .post("/users-service/auth/login")
        .then().statusCode(200).extract().path("accessToken");
    assertTrue(token != null && !token.isBlank(), "Login must return an access token");

    // Verify the candidate account ID because registration only returns the user ID.
    Response account = given().spec(api).auth().oauth2(token)
        .get("/accounts-service/accounts/{id}", registered.jsonPath().getLong("id"))
        .then().statusCode(200)
        .body("cvu", equalTo(registered.jsonPath().getString("cvu")),
            "alias", equalTo(registered.jsonPath().getString("alias")),
            "balance", comparesEqualTo(BigDecimal.ZERO))
        .extract().response();
    return new TestAccount(account.jsonPath().getLong("id"), token);
  }

  private Long createCard(TestAccount account, String number, String type) {
    Response card = request(account).body(Map.of("cardNumber", number, "type", type))
        .post("/accounts/{id}/cards", account.id())
        .then().statusCode(201).body("type", equalTo(type)).extract().response();
    return card.jsonPath().getLong("id");
  }

  private Long createDeposit(Long cardId, BigDecimal amount) {
    Response deposit = request(primary).body(Map.of("cardId", cardId, "amount", amount))
        .post("/accounts/{id}/transferences", primary.id())
        .then().statusCode(201).extract().response();
    Long id = deposit.jsonPath().getLong("id");
    assertTrue(id != null && id > 0, "A deposit must return its transfer ID");
    transferIds.add(id);
    expectedBalance = expectedBalance.add(amount);
    deposit.then().body("amount", comparesEqualTo(amount), "type", equalTo("CREDIT"),
        "balanceAfter", comparesEqualTo(expectedBalance));
    return id;
  }

  private void assertAccountsUnchanged() {
    request(primary).get("/accounts-service/accounts/{id}", primary.id())
        .then().statusCode(200).body("balance", comparesEqualTo(expectedBalance));
    request(primary).get("/accounts/{id}/activity", primary.id())
        .then().statusCode(200).body("size()", equalTo(transferIds.size()));
    request(secondary).get("/accounts-service/accounts/{id}", secondary.id())
        .then().statusCode(200).body("balance", comparesEqualTo(BigDecimal.ZERO));
    request(secondary).get("/accounts/{id}/activity", secondary.id())
        .then().statusCode(200).body("", empty());
  }

  private void deleteCard(TestAccount account, Long id) {
    if (account == null || id == null) return;
    int status = request(account).delete("/cards/accounts/{accountId}/cards/{cardId}", account.id(), id)
        .statusCode();
    assertTrue(status == 200 || status == 404, "Card cleanup returned HTTP " + status);
  }

  private RequestSpecification request(TestAccount account) {
    return given().spec(api).auth().oauth2(account.token());
  }
}
