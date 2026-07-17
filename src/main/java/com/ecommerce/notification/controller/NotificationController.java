package com.ecommerce.notification.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMine(Authentication authentication,
			Pageable pageable) {
		return ResponseEntity
				.ok(ApiResponse.success(notificationService.getMyNotifications(authentication.getName(), pageable)));
	}

	@PutMapping("/{id}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication authentication) {
		notificationService.markAsRead(id, authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
	}
}