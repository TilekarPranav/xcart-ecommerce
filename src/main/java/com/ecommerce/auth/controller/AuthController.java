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
			HttpServletResponse response) {
		AuthResponse tokens = authService.register(request);
		addAuthCookies(response, tokens);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(authService.me(tokens.getEmail()), "Account created successfully"));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<UserSummaryResponse>> login(@Valid @RequestBody LoginRequest request,
			HttpServletResponse response) {
		AuthResponse tokens = authService.login(request);
		addAuthCookies(response, tokens);
		return ResponseEntity.ok(ApiResponse.success(authService.me(tokens.getEmail()), "Login successful"));
	}

	private void addAuthCookies(HttpServletResponse response, AuthResponse tokens) {
		ResponseCookie access = ResponseCookie.from("accessToken", tokens.getAccessToken()).httpOnly(true).secure(true)
				.sameSite("None").path("/").maxAge(15 * 60).build();
		ResponseCookie refresh = ResponseCookie.from("refreshToken", tokens.getRefreshToken()).httpOnly(true)
				.secure(true).sameSite("None").path("/auth/refresh").maxAge(7 * 24 * 60 * 60).build();
		response.addHeader(HttpHeaders.SET_COOKIE, access.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refresh.toString());
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserSummaryResponse>> me(Authentication authentication) {
		UserSummaryResponse response = authService.me(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(response));
	}
	
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<Void>> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken,
			HttpServletResponse response) {
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
				.path("/").maxAge(0).build();
		ResponseCookie refresh = ResponseCookie.from("refreshToken", "").httpOnly(true).secure(true).sameSite("None")
				.path("/auth/refresh").maxAge(0).build();
		response.addHeader(HttpHeaders.SET_COOKIE, access.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refresh.toString());
		return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
	}
}