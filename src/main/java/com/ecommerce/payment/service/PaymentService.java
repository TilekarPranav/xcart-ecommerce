package com.ecommerce.payment.service;

import org.springframework.stereotype.Service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.gateway.PaymentGateway;
import com.ecommerce.payment.gateway.PaymentGatewayResult;
import com.ecommerce.payment.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final OrderRepository orderRepository;
	private final PaymentGateway paymentGateway;

	@Transactional
	public PaymentResponse processPayment(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

		if (paymentRepository.findById(orderId).isPresent()) {
			throw new ConflictException("A payment already exists for this order");
		}

		if (order.getStatus() != OrderStatus.PLACED) {
			throw new BadRequestException("Order must be in PLACED status to process payment");
		}

		Payment payment = Payment.builder().order(order).status(PaymentStatus.PENDING).amount(order.getTotalAmount())
				.build();

		PaymentGatewayResult result = paymentGateway.charge(orderId, order.getTotalAmount());

		if (result.isSuccess()) {
			payment.setStatus(PaymentStatus.SUCCESS);
			payment.setProviderRef(result.getProviderRef());
			order.setStatus(OrderStatus.CONFIRMED);
		} else {
			payment.setStatus(PaymentStatus.FAILED);
		}

		paymentRepository.save(payment);
		orderRepository.save(order);

		return toResponse(payment);

	}

	public PaymentResponse getByOrderId(Long orderId) {
		Payment payment = paymentRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("No payment found for order id: " + orderId));
		return toResponse(payment);
	}

	private PaymentResponse toResponse(Payment payment) {
		return PaymentResponse.builder().id(payment.getId()).orderId(payment.getOrder().getId())
				.status(payment.getStatus()).amount(payment.getAmount()).providerRef(payment.getProviderRef())
				.createdAt(payment.getCreatedAt()).build();
	}
}
