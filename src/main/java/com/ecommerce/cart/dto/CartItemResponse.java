package com.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
	private Long cartItemId;
	private Long productId;
	private String productName;
	private BigDecimal unitPrice;
	private int quantity;
	private BigDecimal subtotal;
}