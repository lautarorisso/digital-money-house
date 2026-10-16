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

class AccountControllerDepositTest {
  private final TransactionService transactions = mock(TransactionService.class);
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("owner").build();
    mvc = MockMvcBuilders.standaloneSetup(new AccountController(
        mock(AccountService.class), mock(CardService.class), transactions))
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
  }

  @Test
  void oldRouteCannotDeposit() throws Exception {
    mvc.perform(post("/accounts/1/transferences").contentType(MediaType.APPLICATION_JSON)
        .content("{\"cardId\":2,\"amount\":10}"))
        .andExpect(status().isNotFound());
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
