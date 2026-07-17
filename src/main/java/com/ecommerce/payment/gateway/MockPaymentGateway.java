package com.ecommerce.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {

	@Override
	public PaymentGatewayResult charge(Long orderId, BigDecimal amount) {

		boolean simulatedSuccess = orderId % 5 != 0;

		if (simulatedSuccess) {
			return PaymentGatewayResult.builder().success(true).providerRef("MOCK-" + UUID.randomUUID()).build();
		} else {
			return PaymentGatewayResult.builder().success(false).providerRef("Simulated decline for testing").build();
		}
	}

}
