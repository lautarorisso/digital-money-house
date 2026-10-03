package com.lautarorisso.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
	"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.invalid/test-realm/certs",
	"eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
