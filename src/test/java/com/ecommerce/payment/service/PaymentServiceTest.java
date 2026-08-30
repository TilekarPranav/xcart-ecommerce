package com.ecommerce.payment.service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.gateway.PaymentGateway;
import com.ecommerce.payment.gateway.PaymentGatewayResult;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.user.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;
	@Mock
	private OrderRepository orderRepository;
	@Mock
	private PaymentGateway paymentGateway;

	@InjectMocks
	private PaymentService paymentService;

	private Order placedOrder;
	private User owner;

	@BeforeEach
	void setUp() {
		owner = User.builder().id(1L).email("owner@test.com").build();
		placedOrder = Order.builder().id(1L).user(owner).status(OrderStatus.PLACED)
				.totalAmount(new BigDecimal("149.99")).build();
	}

	@Test
	void processPayment_success_setsPaymentSuccessAndConfirmsOrder() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(placedOrder));
		when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
		when(paymentGateway.charge(1L, new BigDecimal("149.99")))
				.thenReturn(PaymentGatewayResult.builder().success(true).providerRef("STRIPE-abc123").build());
		when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
			Payment p = inv.getArgument(0);
			p.setId(1L);
			return p;
		});

		var response = paymentService.processPayment(1L, "owner@test.com");

		assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		assertThat(response.getProviderRef()).isEqualTo("STRIPE-abc123");
		assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		verify(paymentRepository).save(any(Payment.class));
		verify(orderRepository).save(placedOrder);
	}

	@Test
	void processPayment_gatewayDeclines_setsPaymentFailedAndKeepsOrderStatus() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(placedOrder));
		when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
		when(paymentGateway.charge(1L, new BigDecimal("149.99")))
				.thenReturn(PaymentGatewayResult.builder().success(false).failureReason("Card declined").build());
		when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
			Payment p = inv.getArgument(0);
			p.setId(1L);
			return p;
		});

		var response = paymentService.processPayment(1L, "owner@test.com");

		assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
		assertThat(response.getProviderRef()).isNull();
		assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.PLACED);
	}

	@Test
	void processPayment_orderNotFound_throwsResourceNotFoundException() {
		when(orderRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentService.processPayment(404L, "owner@test.com"))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("404");
		verify(paymentGateway, never()).charge(any(), any());
	}

	@Test
	void processPayment_paymentAlreadyExists_throwsConflictException() {
		Payment existingPayment = Payment.builder().id(5L).order(placedOrder).status(PaymentStatus.SUCCESS)
				.amount(new BigDecimal("149.99")).build();
		when(orderRepository.findById(1L)).thenReturn(Optional.of(placedOrder));
		when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(existingPayment));

		assertThatThrownBy(() -> paymentService.processPayment(1L, "owner@test.com"))
				.isInstanceOf(ConflictException.class).hasMessageContaining("payment already exists");
		verify(paymentGateway, never()).charge(any(), any());
	}

	@Test
	void processPayment_orderNotInPlacedStatus_throwsBadRequestException() {
		placedOrder.setStatus(OrderStatus.CONFIRMED);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(placedOrder));
		when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentService.processPayment(1L, "owner@test.com"))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("PLACED");
		verify(paymentGateway, never()).charge(any(), any());
	}

	@Test
	void processPayment_notOwner_throwsResourceNotFoundException() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(placedOrder));

		assertThatThrownBy(() -> paymentService.processPayment(1L, "attacker@test.com"))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(paymentGateway, never()).charge(any(), any());
	}

	@Test
	void getByOrderId_whenPaymentExists_returnsPaymentResponse() {
		Payment payment = Payment.builder().id(1L).order(placedOrder).status(PaymentStatus.SUCCESS)
				.amount(new BigDecimal("149.99")).providerRef("STRIPE-abc123").build();
		when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

		var response = paymentService.getByOrderId(1L, "owner@test.com", false);

		assertThat(response.getId()).isEqualTo(1L);
		assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
		assertThat(response.getProviderRef()).isEqualTo("STRIPE-abc123");
	}

	@Test
	void getByOrderId_notOwnerAndNotAdmin_throwsResourceNotFoundException() {
		Payment payment = Payment.builder().id(1L).order(placedOrder).status(PaymentStatus.SUCCESS)
				.amount(new BigDecimal("149.99")).build();
		when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> paymentService.getByOrderId(1L, "attacker@test.com", false))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void getByOrderId_adminCanViewAnyOrdersPayment() {
		Payment payment = Payment.builder().id(1L).order(placedOrder).status(PaymentStatus.SUCCESS)
				.amount(new BigDecimal("149.99")).providerRef("STRIPE-abc123").build();
		when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

		var response = paymentService.getByOrderId(1L, "someone-else@test.com", true);

		assertThat(response.getId()).isEqualTo(1L);
	}

	@Test
	void getByOrderId_whenNoPayment_throwsResourceNotFoundException() {
		when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> paymentService.getByOrderId(999L, "owner@test.com", false))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("No payment found");
	}
}