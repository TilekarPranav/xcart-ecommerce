package com.ecommerce.payment.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(Authentication authentication,
			@Valid @RequestBody PaymentRequest request) {
		PaymentResponse response = paymentService.processPayment(request.getOrderId(), authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(response, "Payment processed"));
	}

	@GetMapping("/{orderId}")
	public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(Authentication authentication,
			@PathVariable Long orderId) {
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		return ResponseEntity
				.ok(ApiResponse.success(paymentService.getByOrderId(orderId, authentication.getName(), isAdmin)));
	}
}