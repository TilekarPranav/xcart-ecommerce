package com.ecommerce.order.service;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.inventory.service.InventoryService;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private CartRepository cartRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private InventoryService inventoryService;
	@Mock
	private OrderEventPublisher orderEventPublisher;

	@InjectMocks
	private OrderService orderService;

	private User user;
	private Product product;
	private Cart cart;
	private CartItem cartItem;
	private Order order;

	@BeforeEach
	void setUp() {
		user = User.builder().id(1L).name("Pranav").email("[pranav@test.com](mailto:pranav@test.com)").build();
		product = Product.builder().id(10L).name("Laptop").price(new BigDecimal("999.99")).build();
		cartItem = CartItem.builder().id(1L).cart(cart).product(product).quantity(2)
				.unitPriceSnapshot(new BigDecimal("999.99")).build();
		cart = Cart.builder().id(1L).user(user).build();
		cart.getItems().add(cartItem);
		cartItem.setCart(cart);
		order = Order.builder().id(1L).user(user).status(OrderStatus.PLACED)
				.totalAmount(new BigDecimal("1999.98")).build();
		OrderItem orderItem = OrderItem.builder().id(1L).order(order).product(product)
				.quantity(2).unitPrice(new BigDecimal("999.99")).build();
		order.setItems(List.of(orderItem));
	}

	@Test
	void placeOrder_success_decreasesStock_andClearsCart() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
			Order o = inv.getArgument(0); o.setId(1L); return o;
		});
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);

		var response = orderService.placeOrder("[pranav@test.com](mailto:pranav@test.com)");

		assertThat(response.getStatus()).isEqualTo(OrderStatus.PLACED);
		assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1999.98"));
		assertThat(response.getItems()).hasSize(1);
		verify(inventoryService).decreaseStockForOrder(10L, 2);
		verify(cartRepository).save(any(Cart.class));
		assertThat(cart.getItems()).isEmpty();
	}

	@Test
	void placeOrder_userNotFound_throwsResourceNotFoundException() {
		when(userRepository.findByEmail("[ghost@test.com](mailto:ghost@test.com)")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> orderService.placeOrder("[ghost@test.com](mailto:ghost@test.com)"))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("User not found");
		verify(cartRepository, never()).findByUserId(any());
	}

	@Test
	void placeOrder_noCart_throwsBadRequestException() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> orderService.placeOrder("[pranav@test.com](mailto:pranav@test.com)"))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("Cart is empty");
	}

	@Test
	void placeOrder_emptyCart_throwsBadRequestException() {
		Cart emptyCart = Cart.builder().id(1L).user(user).build();
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));
		assertThatThrownBy(() -> orderService.placeOrder("[pranav@test.com](mailto:pranav@test.com)"))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("empty cart");
	}

	@Test
	void getMyOrders_returnsPageOfOrdersForUser() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		Pageable pageable = PageRequest.of(0, 10);
		Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);
		when(orderRepository.findByUserId(1L, pageable)).thenReturn(orderPage);

		Page<OrderResponse> result = orderService.getMyOrders("[pranav@test.com](mailto:pranav@test.com)", pageable);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
	}

	@Test
	void getMyOrders_userNotFound_throwsResourceNotFoundException() {
		when(userRepository.findByEmail("[ghost@test.com](mailto:ghost@test.com)")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> orderService.getMyOrders("[ghost@test.com](mailto:ghost@test.com)", PageRequest.of(0, 10)))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("User not found");
	}

	@Test
	void getOrderById_asOwner_returnsOrder() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		var response = orderService.getOrderById(1L, "[pranav@test.com](mailto:pranav@test.com)", false);
		assertThat(response.getId()).isEqualTo(1L);
	}

	@Test
	void getOrderById_asAdmin_returnsAnyOrder() {
		User otherUser = User.builder().id(99L).name("Other").email("[other@test.com](mailto:other@test.com)").build();
		Order otherOrder = Order.builder().id(2L).user(otherUser).status(OrderStatus.PLACED)
				.totalAmount(BigDecimal.TEN).items(List.of()).build();
		when(orderRepository.findById(2L)).thenReturn(Optional.of(otherOrder));
		var response = orderService.getOrderById(2L, "[admin@test.com](mailto:admin@test.com)", true);
		assertThat(response.getId()).isEqualTo(2L);
	}

	@Test
	void getOrderById_nonOwnerNonAdmin_throwsResourceNotFoundException() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		assertThatThrownBy(() -> orderService.getOrderById(1L, "[intruder@test.com](mailto:intruder@test.com)", false))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Order not found");
	}

	@Test
	void getOrderById_orderNotFound_throwsResourceNotFoundException() {
		when(orderRepository.findById(999L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> orderService.getOrderById(999L, "[pranav@test.com](mailto:pranav@test.com)", false))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("999");
	}

	@Test
	void updateStatus_placedToConfirmed_succeeds() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		var response = orderService.updateStatus(1L, OrderStatus.CONFIRMED);
		assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		verify(orderEventPublisher).publishOrderStatusEvent(1L, 1L, "CONFIRMED");
		verify(inventoryService, never()).restockForCancelledOrder(any(), anyInt());
	}

	@Test
	void updateStatus_confirmedToShipped_succeeds() {
		order.setStatus(OrderStatus.CONFIRMED);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		var response = orderService.updateStatus(1L, OrderStatus.SHIPPED);
		assertThat(response.getStatus()).isEqualTo(OrderStatus.SHIPPED);
		verify(orderEventPublisher).publishOrderStatusEvent(1L, 1L, "SHIPPED");
	}

	@Test
	void updateStatus_placedToCancelled_restocksInventory() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		var response = orderService.updateStatus(1L, OrderStatus.CANCELLED);
		assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		verify(inventoryService).restockForCancelledOrder(10L, 2);
		verify(orderEventPublisher).publishOrderStatusEvent(1L, 1L, "CANCELLED");
	}

	@Test
	void updateStatus_invalidTransition_throwsBadRequestException() {
		order.setStatus(OrderStatus.DELIVERED);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.CONFIRMED))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("Cannot transition");
		verify(orderRepository, never()).save(any());
	}

	@Test
	void updateStatus_deliveredHasNoTransitions() {
		order.setStatus(OrderStatus.DELIVERED);
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.CANCELLED))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void updateStatus_orderNotFound_throwsResourceNotFoundException() {
		when(orderRepository.findById(404L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> orderService.updateStatus(404L, OrderStatus.CONFIRMED))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("404");
	}

	@Test
	void cancelOrder_asOwner_delegatesToUpdateStatus() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		orderService.cancelOrder(1L, "[pranav@test.com](mailto:pranav@test.com)");
		verify(orderEventPublisher).publishOrderStatusEvent(1L, 1L, "CANCELLED");
		verify(inventoryService).restockForCancelledOrder(10L, 2);
	}

	@Test
	void cancelOrder_nonOwner_throwsResourceNotFoundException() {
		when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
		assertThatThrownBy(() -> orderService.cancelOrder(1L, "[intruder@test.com](mailto:intruder@test.com)"))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Order not found");
		verify(orderEventPublisher, never()).publishOrderStatusEvent(any(), any(), any());
	}
}
