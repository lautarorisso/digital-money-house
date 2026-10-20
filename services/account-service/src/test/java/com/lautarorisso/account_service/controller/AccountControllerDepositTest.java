package com.lautarorisso.account_service.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lautarorisso.account_service.dto.CreateTransferenceRequest;
import com.lautarorisso.account_service.dto.TransactionResponse;
import com.lautarorisso.account_service.service.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(
    controllers = AccountController.class,
    properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused")
@org.springframework.context.annotation.Import(com.lautarorisso.account_service.config.SecurityConfig.class)
class AccountControllerDepositTest {
  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private AccountService accounts;
  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private CardService cards;
  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private org.springframework.security.oauth2.jwt.JwtDecoder decoder;
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.security.web.FilterChainProxy security;
  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private TransactionService transactions;
  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private InternalTransferService transfers;
  private final ch.qos.logback.classic.Logger logger =
      (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AccountController.class);
  private final ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> logs =
      new ch.qos.logback.core.read.ListAppender<>();
  private MockMvc mvc;

  @org.junit.jupiter.api.AfterEach
  void detachLogs() {
    logger.detachAppender(logs);
    logs.stop();
  }

  @BeforeEach
  void setUp() {
    logs.start();
    logger.addAppender(logs);
    Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("owner").build();
    mvc = MockMvcBuilders.standaloneSetup(new AccountController(
        mock(AccountService.class), mock(CardService.class), transactions, transfers))
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
          }
          public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
              NativeWebRequest request, WebDataBinderFactory factory) {
            return jwt;
          }
        }).build();
  }

  @Test
  void documentationBypassesAuthenticationButAccountsDoNot() throws Exception {
    var securedMvc = MockMvcBuilders.standaloneSetup().addFilters(security).build();
    for (String path : new String[]{"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config"}) {
      securedMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
          .andExpect(status().isNotFound());
    }
    securedMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/accounts/1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void depositsRetainsBodyAndCreatedResponse() throws Exception {
    when(transactions.createTransference(eq(1L), eq("owner"), any()))
        .thenReturn(new TransactionResponse(9L, BigDecimal.TEN, "CREDIT", "Card deposit",
            LocalDateTime.of(2026, 1, 1, 12, 0), BigDecimal.TEN));
    mvc.perform(post("/accounts/1/deposits").contentType(MediaType.APPLICATION_JSON)
        .content("{\"cardId\":2,\"amount\":10}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("id").value(9))
        .andExpect(jsonPath("amount").value(10)).andExpect(jsonPath("type").value("CREDIT"))
        .andExpect(jsonPath("description").value("Card deposit"))
        .andExpect(jsonPath("balanceAfter").value(10));
    verify(transactions).createTransference(1L, "owner",
        new CreateTransferenceRequest(2L, BigDecimal.TEN));
    org.junit.jupiter.api.Assertions.assertEquals(1, logs.list.size());
    org.junit.jupiter.api.Assertions.assertEquals(ch.qos.logback.classic.Level.INFO, logs.list.get(0).getLevel());
    org.junit.jupiter.api.Assertions.assertEquals(
        "event=deposit accountId=1 transactionId=9 amount=10", logs.list.get(0).getFormattedMessage());
  }

  @Test
  void transferLogsOnlyAfterSuccessWithoutDestination() throws Exception {
    mvc.perform(post("/accounts/1/transferences").contentType(MediaType.APPLICATION_JSON)
        .content("{\"destination\":\"private.destination.alias\",\"amount\":10}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("ok"));
    verify(transfers).transfer(1L, "owner", new com.lautarorisso.account_service.dto.InternalTransferRequest(
        "private.destination.alias", BigDecimal.TEN));
    org.junit.jupiter.api.Assertions.assertEquals(1, logs.list.size());
    org.junit.jupiter.api.Assertions.assertEquals(ch.qos.logback.classic.Level.INFO, logs.list.get(0).getLevel());
    org.junit.jupiter.api.Assertions.assertEquals(
        "event=transfer accountId=1 amount=10", logs.list.get(0).getFormattedMessage());
  }

  @Test
  void failedMoneyOperationsDoNotLogSuccess() throws Exception {
    doThrow(new IllegalStateException("commit failed")).when(transactions)
        .createTransference(eq(1L), eq("owner"), any());
    doThrow(new IllegalStateException("commit failed")).when(transfers)
        .transfer(eq(1L), eq("owner"), any());
    org.junit.jupiter.api.Assertions.assertThrows(jakarta.servlet.ServletException.class,
        () -> mvc.perform(post("/accounts/1/deposits").contentType(MediaType.APPLICATION_JSON)
            .content("{\"cardId\":2,\"amount\":10}")));
    org.junit.jupiter.api.Assertions.assertThrows(jakarta.servlet.ServletException.class,
        () -> mvc.perform(post("/accounts/1/transferences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"destination\":\"private.destination.alias\",\"amount\":10}")));
    org.junit.jupiter.api.Assertions.assertTrue(logs.list.isEmpty());
  }

  @Test
  void oldRouteCannotDeposit() throws Exception {
    mvc.perform(post("/accounts/1/transferences").contentType(MediaType.APPLICATION_JSON)
        .content("{\"cardId\":2,\"amount\":10}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(transactions);
  }

  @Test
  void depositsRetainsAmountValidation() throws Exception {
    for (String amount : new String[]{"0", "-10", "0.001", "10000000000"}) {
      mvc.perform(post("/accounts/1/deposits").contentType(MediaType.APPLICATION_JSON)
          .content("{\"cardId\":2,\"amount\":" + amount + "}"))
          .andExpect(status().isBadRequest());
    }
    mvc.perform(post("/accounts/1/deposits").contentType(MediaType.APPLICATION_JSON)
        .content("{}" )).andExpect(status().isBadRequest());
    verifyNoInteractions(transactions);
  }
}
