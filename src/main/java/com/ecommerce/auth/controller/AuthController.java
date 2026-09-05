package com.ecommerce.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserSummaryResponse;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserSummaryResponse>> register(@Valid @RequestBody RegisterRequest request,
			HttpServletResponse response, CsrfToken csrfToken) {
		AuthResponse tokens = authService.register(request);
		addAuthCookies(response, tokens);
		reissueCsrfToken(csrfToken);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(authService.me(tokens.getEmail()), "Account created successfully"));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<UserSummaryResponse>> login(@Valid @RequestBody LoginRequest request,
			HttpServletResponse response, CsrfToken csrfToken) {
		AuthResponse tokens = authService.login(request);
		addAuthCookies(response, tokens);
		reissueCsrfToken(csrfToken);
		return ResponseEntity.ok(ApiResponse.success(authService.me(tokens.getEmail()), "Login successful"));
	}

	/**
	 * authService.login() calls authenticationManager.authenticate(...)
	 * directly, which Spring Security's own CSRF docs note clears the CSRF
	 * cookie as a side effect of a successful authentication. Resolving
	 * csrfToken.getToken() here forces a fresh token to be generated and saved
	 * on THIS response, after any such clearing has already happened.
	 */
	private void reissueCsrfToken(CsrfToken csrfToken) {
		csrfToken.getToken();
	}

	// SameSite=None + Partitioned, not Lax: the frontend calls this backend
	// DIRECTLY (see API_BASE_URL in the frontend's constants/app.ts), not through
	// Render's static-site /api/* proxy — that proxy was tried and empirically
	// ruled out (a direct curl test worked; every attempt through the proxy lost
	// the CSRF cookie specifically). x-cart.onrender.com and
	// xcart-ecommerce.onrender.com are genuinely different sites, so these
	// cookies need SameSite=None to be sent on this cross-site request at all,
	// and Partitioned (CHIPS) so browsers phasing out third-party cookies don't
	// block them. Keep all three cookies (this one, refreshToken, and the CSRF
	// cookie in PartitionedCookieCsrfTokenRepository) consistent with each other.
	private void addAuthCookies(HttpServletResponse response, AuthResponse tokens) {
		ResponseCookie access = ResponseCookie.from("accessToken", tokens.getAccessToken()).httpOnly(true).secure(true)
				.sameSite("None").partitioned(true).path("/").maxAge(15 * 60).build();
		ResponseCookie refresh = ResponseCookie.from("refreshToken", tokens.getRefreshToken()).httpOnly(true)
				.secure(true).sameSite("None").partitioned(true).path("/auth/refresh").maxAge(7 * 24 * 60 * 60).build();
		response.addHeader(HttpHeaders.SET_COOKIE, access.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refresh.toString());
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserSummaryResponse>> me(Authentication authentication) {
		UserSummaryResponse response = authService.me(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<Void>> refresh(
			@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {
		if (refreshToken == null) {
			throw new com.ecommerce.exception.BadRequestException("No refresh token provided");
		}
		AuthResponse tokens = authService.refresh(refreshToken);
		addAuthCookies(response, tokens);
		return ResponseEntity.ok(ApiResponse.success(null, "Token refreshed"));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
		// httpOnly cookies can't be cleared by client-side JS — this endpoint is the
		// only way to actually end the session. maxAge(0) tells the browser to
		// delete the cookie immediately.
		ResponseCookie access = ResponseCookie.from("accessToken", "").httpOnly(true).secure(true).sameSite("None")
				.partitioned(true).path("/").maxAge(0).build();
		ResponseCookie refresh = ResponseCookie.from("refreshToken", "").httpOnly(true).secure(true).sameSite("None")
				.partitioned(true).path("/auth/refresh").maxAge(0).build();
		response.addHeader(HttpHeaders.SET_COOKIE, access.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refresh.toString());
		return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
	}

	@GetMapping("/csrf")
	public ResponseEntity<ApiResponse<String>> csrf(CsrfToken csrfToken) {
		return ResponseEntity.ok(ApiResponse.success(csrfToken.getToken(), "CSRF token initialized"));
	}
}