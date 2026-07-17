package com.ecommerce.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.user.dto.ChangePasswordRequest;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/profile")
	public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
		UserProfileResponse response = userService.getProfile(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@PutMapping("/profile")
	public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(Authentication authentication,
			@Valid @RequestBody UpdateProfileRequest request) {
		UserProfileResponse response = userService.updateProfile(authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.success(response, "Profile updated successfully"));
	}

	@PutMapping("/change-password")
	public ResponseEntity<ApiResponse<Void>> changePassword(Authentication authentication,
			@Valid @RequestBody ChangePasswordRequest request) {
		userService.changePassword(authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
	}
}
