package com.ecommerce.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.ecommerce.support.AbstractIntegrationTest;

/**
 * Kept as its own test class (rather than a method on AuthControllerTest) because it
 * needs a different Spring context — a real 15-minute expiry isn't practical to wait
 * out in a test, so this overrides app.jwt.access-token-expiration-ms down to
 * effectively nothing for just this class.
 */
@TestPropertySource(properties = "app.jwt.access-token-expiration-ms=1")
class ExpiredAccessTokenTest extends AbstractIntegrationTest {

	@Test
	void protectedEndpoint_withExpiredAccessToken_returns401() throws Exception {
		AuthedUser user = registerUser();
		Thread.sleep(25); // the 1ms expiry above has already elapsed by the time this runs

		mockMvc.perform(get("/auth/me").cookie(user.accessToken())).andExpect(status().isUnauthorized());
	}
}
