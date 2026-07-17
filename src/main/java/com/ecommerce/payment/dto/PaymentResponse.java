package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.PaymentStatus;
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
public class PaymentResponse {
	private Long id;
	private Long orderId;
	private PaymentStatus status;
	private BigDecimal amount;
	private String providerRef;
	private Instant createdAt;
}