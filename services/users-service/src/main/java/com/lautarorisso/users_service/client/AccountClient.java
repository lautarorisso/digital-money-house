package com.lautarorisso.users_service.client;

import com.lautarorisso.users_service.dto.AccountResponse;
import com.lautarorisso.users_service.dto.CreateAccountRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountClient {

  @PostMapping("/accounts")
  AccountResponse createAccount(@RequestBody CreateAccountRequest request);
}