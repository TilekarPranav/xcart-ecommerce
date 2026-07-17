package com.ecommerce.admin.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

	private final OrderService orderService;

	@GetMapping
	public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(pageable)));
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id,
			@Valid @RequestBody UpdateOrderStatusRequest request) {
		OrderResponse response = orderService.updateStatus(id, request.getStatus());
		return ResponseEntity.ok(ApiResponse.success(response, "Order status updated"));
	}
}