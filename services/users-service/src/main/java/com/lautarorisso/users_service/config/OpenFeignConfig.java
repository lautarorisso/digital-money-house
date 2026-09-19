package com.lautarorisso.users_service.config;

import com.lautarorisso.users_service.client.KeycloakClient;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class OpenFeignConfig {

  @Bean
  RequestInterceptor feignAuthorizationInterceptor(KeycloakClient keycloakClient) {
    return template -> template.header(HttpHeaders.AUTHORIZATION,
        "Bearer " + keycloakClient.getServiceAccountToken());
  }
}