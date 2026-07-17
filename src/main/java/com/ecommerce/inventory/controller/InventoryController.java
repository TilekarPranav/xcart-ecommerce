package com.ecommerce.inventory.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	@GetMapping("/{productId}")
	public ResponseEntity<ApiResponse<InventoryResponse>> getByProductId(@PathVariable Long productId) {
		return ResponseEntity.ok(ApiResponse.success(inventoryService.getByProductId(productId)));
	}

	@PutMapping("/update")
	public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(
			@Valid @RequestBody InventoryUpdateRequest request) {
		InventoryResponse response = inventoryService.updateStock(request);
		return ResponseEntity.ok(ApiResponse.success(response, "Inventory updated successfully"));
	}
}