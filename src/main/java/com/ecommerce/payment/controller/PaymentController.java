package com.ecommerce.payment.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
		PaymentResponse response = paymentService.processPayment(request.getOrderId());
		return ResponseEntity.ok(ApiResponse.success(response, "Payment processed"));
	}

	@GetMapping("/{orderId}")
	public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success(paymentService.getByOrderId(orderId)));
	}
}