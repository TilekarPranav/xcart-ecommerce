package com.ecommerce.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.ecommerce.support.AbstractIntegrationTest;

import jakarta.servlet.http.Cookie;

/**
 * Controller-level tests for AuthController. Covers login, register, token-type
 * separation, CSRF requirements for stateful endpoints, and 401 handling for
 * bad/unknown credentials.
 */
class AuthControllerTest extends AbstractIntegrationTest {

	@Test
	void register_success_setsBothAuthCookiesAndReturnsProfile() throws Exception {
		String email = "new-" + UUID.randomUUID() + "@test.com";
		String body = """
				{"name":"New User","email":"%s","password":"Password123!"}
				""".formatted(email);

		mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.data.email").value(email)).andExpect(result -> {
					Cookie access = result.getResponse().getCookie("accessToken");
					Cookie refresh = result.getResponse().getCookie("refreshToken");
					org.junit.jupiter.api.Assertions.assertNotNull(access, "accessToken cookie must be set");
					org.junit.jupiter.api.Assertions.assertNotNull(refresh, "refreshToken cookie must be set");
					org.junit.jupiter.api.Assertions.assertTrue(access.isHttpOnly());
					org.junit.jupiter.api.Assertions.assertTrue(refresh.isHttpOnly());
				});
	}

	@Test
	void register_duplicateEmail_returnsConflict() throws Exception {
		AuthedUser existing = registerUser();
		String body = """
				{"name":"Someone Else","email":"%s","password":"Password123!"}
				""".formatted(existing.email());

		mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void login_validCredentials_returns200AndSetsCookies() throws Exception {
		AuthedUser user = registerUser();
		String body = """
				{"email":"%s","password":"%s"}
				""".formatted(user.email(), user.password());

		mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(user.email()));
	}

	@Test
	void login_wrongPassword_returns401Unauthorized() throws Exception {
		AuthedUser user = registerUser();
		String body = """
				{"email":"%s","password":"wrong-password-entirely"}
				""".formatted(user.email());

		mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void login_unknownEmail_returns401Unauthorized() throws Exception {
		String body = """
				{"email":"nobody-%s@test.com","password":"whatever123"}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void protectedEndpoint_withoutAnyToken_returns401() throws Exception {
		mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_withValidAccessToken_returns200() throws Exception {
		AuthedUser user = registerUser();
		mockMvc.perform(get("/auth/me").cookie(user.accessToken())).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(user.email()));
	}

	/**
	 * Regression test: JwtAuthenticationFilter enforces type=="access", so a
	 * refresh token (7-day lifetime) presented as the access token is rejected.
	 */
	@Test
	void protectedEndpoint_withRefreshTokenInPlaceOfAccessToken_returns401() throws Exception {
		AuthedUser user = registerUser();
		Cookie refreshPresentedAsAccess = new Cookie("accessToken", user.refreshToken().getValue());

		mockMvc.perform(get("/auth/me").cookie(refreshPresentedAsAccess)).andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpoint_withRefreshTokenAsAuthorizationHeader_returns401() throws Exception {
		AuthedUser user = registerUser();

		mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + user.refreshToken().getValue()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refresh_withValidRefreshToken_returns200AndIssuesNewCookies() throws Exception {
		AuthedUser user = registerUser();
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(
				post("/auth/refresh").cookie(user.refreshToken()).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk()).andExpect(result -> {
					Cookie newAccess = result.getResponse().getCookie("accessToken");
					org.junit.jupiter.api.Assertions.assertNotNull(newAccess);
				});
	}

	@Test
	void refresh_withoutRefreshTokenCookie_returns400() throws Exception {
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(post("/auth/refresh").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void refresh_withAccessTokenInsteadOfRefreshToken_isRejected() throws Exception {
		AuthedUser user = registerUser();
		Cookie accessPresentedAsRefresh = new Cookie("refreshToken", user.accessToken().getValue());
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(post("/auth/refresh").cookie(accessPresentedAsRefresh).cookie(csrf).header("X-XSRF-TOKEN",
				csrf.getValue())).andExpect(status().is4xxClientError())
				.andExpect(jsonPath("$.message", containsString("token type")));
	}

	@Test
	void refresh_withoutCsrfToken_isRejected() throws Exception {
		AuthedUser user = registerUser();

		mockMvc.perform(post("/auth/refresh").cookie(user.refreshToken())).andExpect(status().isForbidden());
	}

	@Test
	void logout_clearsBothCookiesWithMaxAgeZero() throws Exception {
		AuthedUser user = registerUser();
		Cookie csrf = fetchCsrfCookie();

		MvcResult result = mockMvc.perform(
				post("/auth/logout").cookie(user.accessToken()).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk()).andReturn();

		Cookie access = result.getResponse().getCookie("accessToken");
		Cookie refresh = result.getResponse().getCookie("refreshToken");
		org.junit.jupiter.api.Assertions.assertEquals(0, access.getMaxAge());
		org.junit.jupiter.api.Assertions.assertEquals(0, refresh.getMaxAge());
	}

	@Test
	void logout_withoutCsrfToken_isRejected() throws Exception {
		AuthedUser user = registerUser();

		mockMvc.perform(post("/auth/logout").cookie(user.accessToken())).andExpect(status().isForbidden());
	}

	/**
	 * Regression test for a real bug: the refreshToken cookie was previously set
	 * with Path=/api/auth/refresh, but the actual endpoint is /auth/refresh (no
	 * /api prefix — this app's frontend calls the backend origin directly, it
	 * doesn't route through any /api proxy). A browser only attaches a cookie to
	 * requests whose path falls under the cookie's own Path, so a mismatch here
	 * means the browser silently never sends the cookie back — MockMvc's
	 * .cookie(Cookie) doesn't enforce this, so it must be asserted explicitly
	 * against the literal Set-Cookie header rather than relying on .cookie(...)
	 * still "working" in a test.
	 */
	@Test
	void refreshCookie_pathMatchesTheActualRefreshEndpoint() throws Exception {
		AuthedUser user = registerUser();
		Cookie refresh = user.refreshToken();
		org.junit.jupiter.api.Assertions.assertEquals("/auth/refresh", refresh.getPath(),
				"refreshToken cookie's Path must match the real /auth/refresh route, or browsers will never send it back");
	}

	@Test
	void logout_clearsRefreshCookie_atTheSamePathItWasSetWith() throws Exception {
		AuthedUser user = registerUser();
		Cookie csrf = fetchCsrfCookie();

		MvcResult result = mockMvc.perform(
				post("/auth/logout").cookie(user.accessToken()).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk()).andReturn();

		Cookie clearedRefresh = result.getResponse().getCookie("refreshToken");
		org.junit.jupiter.api.Assertions.assertEquals(user.refreshToken().getPath(), clearedRefresh.getPath(),
				"logout must clear the cookie at the exact Path it was set with, or the browser treats this as an "
						+ "unrelated cookie and the real refreshToken cookie is never deleted");
	}
}
