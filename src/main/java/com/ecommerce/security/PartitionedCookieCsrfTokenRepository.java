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
 * Why not just use CookieCsrfTokenRepository directly: this frontend and
 * backend are deployed as two different sites (x-cart.onrender.com /
 * xcart-ecommerce.onrender.com). Without SameSite=None, the browser will never
 * send this cookie on the cross-site XHR/fetch requests this app depends on —
 * the exact failure mode that previously broke cross-site auth entirely (see
 * AuthController's own cookie config and its history). Rolling this small
 * repository instead of relying on a Spring-Security-version-specific cookie
 * customizer hook keeps the fix independent of the exact Spring Security minor
 * version this project resolves.
 *
 * Security model: the cookie is readable by JavaScript (httpOnly=false) —
 * that's required, since the frontend must read it and echo it back in the
 * X-XSRF-TOKEN header. That's normal and expected for the double-submit
 * pattern: a cross-site attacker can trigger a request that *carries* the
 * cookie automatically, but same-origin policy prevents them from *reading* its
 * value to put in the header themselves.
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
