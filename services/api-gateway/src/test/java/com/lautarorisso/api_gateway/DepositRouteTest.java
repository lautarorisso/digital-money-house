package com.lautarorisso.api_gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class DepositRouteTest {

  @Test
  void depositsReplaceTransferencesWithoutChangingSiblingRoutes() throws IOException {
    PropertySource<?> configuration = new YamlPropertySourceLoader()
        .load("gateway", new ClassPathResource("application.yaml")).get(0);
    String prefix = "spring.cloud.gateway.server.webmvc.routes";
    Map<String, String> routes = new LinkedHashMap<>();
    for (int i = 0; configuration.getProperty(prefix + "[" + i + "].id") != null; i++) {
      String route = prefix + "[" + i + "]";
      String id = (String) configuration.getProperty(route + ".id");
      assertEquals(id.equals("users-service") ? "lb://users-service" : "lb://account-service",
          configuration.getProperty(route + ".uri"));
      routes.put(id, (String) configuration.getProperty(route + ".predicates[0]"));
    }

    assertEquals(Map.of(
        "users-service", "Path=/users-service/**",
        "account-service", "Path=/accounts-service/**",
        "account-service-card-endpoints",
        "Path=/cards,/accounts/*/cards,/cards/accounts/*/cards,/cards/accounts/*/cards/*",
        "account-service-activity-endpoints", "Path=/accounts/*/activity,/accounts/*/activity/*",
        "account-service-deposit-endpoints", "Path=/accounts/*/deposits"), routes);
    assertEquals("RewritePath=/users-service/(?<segment>.*), /${segment}",
        configuration.getProperty(prefix + "[0].filters[0]"));
    assertEquals("RewritePath=/accounts-service/(?<segment>.*), /${segment}",
        configuration.getProperty(prefix + "[1].filters[0]"));
  }
}
