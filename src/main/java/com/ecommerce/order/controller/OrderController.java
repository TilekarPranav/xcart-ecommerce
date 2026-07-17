package com.ecommerce.order.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(Authentication authentication) {
		OrderResponse response = orderService.placeOrder(authentication.getName());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response, "Order placed successfully"));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(Authentication authentication,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(authentication.getName(), pageable)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(Authentication authentication,
			@PathVariable Long id) {
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id, authentication.getName(), isAdmin)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> cancelOrder(Authentication authentication, @PathVariable Long id) {
		orderService.cancelOrder(id, authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(null, "Order cancelled"));
	}
}