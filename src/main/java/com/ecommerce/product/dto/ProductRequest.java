package com.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

	@NotBlank(message = "Product name is required")
	private String name;

	@Size(max = 2000, message = "Description must be under 2000 characters")
	private String description;

	@NotNull(message = "Price is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
	private BigDecimal price;

	private String imageUrl;

	@NotNull(message = "Category ID is required")
	private Long categoryId;
}