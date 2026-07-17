package com.ecommerce.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.user.dto.AdminUserResponse;
import com.ecommerce.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserService userService;

	@GetMapping
	public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> listUsers(Pageable pageable) {
		Page<AdminUserResponse> users = userService.listUsers(pageable);
		return ResponseEntity.ok(ApiResponse.success(users));
	}

	@PutMapping("/{id}/disable")
	public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long id) {
		userService.disableUser(id);
		return ResponseEntity.ok(ApiResponse.success(null, "User disabled"));
	}

	@PutMapping("/{id}/enable")
	public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long id) {
		userService.enableUser(id);
		return ResponseEntity.ok(ApiResponse.success(null, "User enabled"));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
	}
}
