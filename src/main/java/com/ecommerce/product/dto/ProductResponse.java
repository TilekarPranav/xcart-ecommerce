package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
	private Long id;
	private String name;
	private String description;
	private BigDecimal price;
	private String imageUrl;
	private boolean active;
	private Long categoryId;
	private String categoryName;
	private Instant createdAt;
}