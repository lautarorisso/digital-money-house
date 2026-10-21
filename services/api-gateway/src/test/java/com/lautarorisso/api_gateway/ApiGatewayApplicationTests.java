package com.lautarorisso.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.invalid/test-realm/certs",
	"eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	org.springframework.security.web.FilterChainProxy security;

	@Test
	void onlyPrefixedDocumentationBypassesAuthentication() throws Exception {
		var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup()
			.addFilters(security).build();
		for (String prefix : new String[]{"/users-service", "/accounts-service"}) {
			for (String path : new String[]{"/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config"}) {
				mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(prefix + path))
					.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
			}
		}
		for (String path : new String[]{"/v3/api-docs", "/swagger-ui/index.html", "/users-service/users/1", "/accounts-service/accounts/1"}) {
			mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
		}
	}

	@Test
	void contextLoads() {
	}

}
