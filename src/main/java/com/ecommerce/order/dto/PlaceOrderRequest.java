package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PlaceOrderRequest {

	@Pattern(regexp = "STANDARD|EXPRESS", message = "shippingMethod must be STANDARD or EXPRESS")
	private String shippingMethod = "STANDARD";

	@NotBlank(message = "Shipping address is required")
	private String shippingAddress;
}