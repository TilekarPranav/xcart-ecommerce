package com.ecommerce.security;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cookie-based CSRF token repository (double-submit cookie pattern),
 * functionally equivalent to Spring Security's own
 * CookieCsrfTokenRepository.withHttpOnlyFalse(), except that the cookie it
 * writes carries the same SameSite=None; Secure; Partitioned attributes
 * AuthController already uses for accessToken/refreshToken.
 *
 * SameSite=None + Partitioned, not Lax: the frontend calls this backend
 * DIRECTLY (https://xcart-ecommerce.onrender.com), not through Render's
 * static-site /api/* proxy rewrite. That proxy was tried and empirically
 * ruled out — a direct curl test (bypassing the proxy) round-tripped this
 * exact cookie correctly, while every attempt through the proxy silently
 * lost it, for reasons outside this app's control. Since x-cart.onrender.com
 * and xcart-ecommerce.onrender.com are genuinely different sites, this
 * cookie needs SameSite=None to be sent back on that cross-site request at
 * all, and Partitioned (CHIPS) so it isn't treated as a blockable
 * third-party cookie by browsers phasing those out.
 */
public class PartitionedCookieCsrfTokenRepository implements CsrfTokenRepository {

	public static final String DEFAULT_COOKIE_NAME = "XSRF-TOKEN";
	public static final String DEFAULT_HEADER_NAME = "X-XSRF-TOKEN";
	public static final String DEFAULT_PARAMETER_NAME = "_csrf";

	@Override
	public CsrfToken generateToken(HttpServletRequest request) {
		return new DefaultCsrfToken(DEFAULT_HEADER_NAME, DEFAULT_PARAMETER_NAME, UUID.randomUUID().toString());
	}

	@Override
	public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
		boolean isDeleting = token == null;
		ResponseCookie cookie = ResponseCookie.from(DEFAULT_COOKIE_NAME, isDeleting ? "" : token.getToken())
				.httpOnly(false).secure(true).sameSite("None").partitioned(true).path("/")
				.maxAge(isDeleting ? 0 : 60 * 60).build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	@Override
	public CsrfToken loadToken(HttpServletRequest request) {
		Cookie cookie = getCookie(request);
		if (cookie == null || cookie.getValue() == null || cookie.getValue().isEmpty()) {
			return null;
		}
		return new DefaultCsrfToken(DEFAULT_HEADER_NAME, DEFAULT_PARAMETER_NAME, cookie.getValue());
	}

	private Cookie getCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie c : cookies) {
			if (DEFAULT_COOKIE_NAME.equals(c.getName())) {
				return c;
			}
		}
		return null;
	}
}