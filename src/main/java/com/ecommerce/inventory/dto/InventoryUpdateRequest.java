package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {

	@NotNull(message = "Product ID is required")
	private Long productId;

	@NotNull(message = "Quantity is required")
	private Integer quantity;

	// "SET", "ADD", or "REDUCE" - tells the service how to interpret quantity
	@NotNull(message = "Operation type is required")
	private String operation;
}
