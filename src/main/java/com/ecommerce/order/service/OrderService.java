package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.inventory.service.InventoryService;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final CartRepository cartRepository;
	private final UserRepository userRepository;
	private final InventoryService inventoryService;
	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(OrderStatus.PLACED,
			Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED), OrderStatus.CONFIRMED,
			Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED), OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
			OrderStatus.DELIVERED, Set.of(), OrderStatus.CANCELLED, Set.of());
	private final OrderEventPublisher orderEventPublisher;

	private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50");
	private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("6.99");
	private static final BigDecimal EXPRESS_SHIPPING = new BigDecimal("14.99");
	private static final BigDecimal TAX_RATE = new BigDecimal("0.08");

	@Transactional
	public OrderResponse placeOrder(String email, PlaceOrderRequest request) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Cart cart = cartRepository.findByUserId(user.getId())
				.orElseThrow(() -> new BadRequestException("Cart is empty"));

		if (cart.getItems().isEmpty()) {
			throw new BadRequestException("Cannot place an order with an empty cart");
		}

		for (CartItem cartItem : cart.getItems()) {
			inventoryService.decreaseStockForOrder(cartItem.getProduct().getId(), cartItem.getQuantity());
		}

		List<OrderItem> orderItems = cart.getItems().stream().map(item -> OrderItem.builder().product(item.getProduct())
				.quantity(item.getQuantity()).unitPrice(item.getUnitPriceSnapshot()).build()).toList();

		BigDecimal subtotal = orderItems.stream()
				.map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		boolean express = "EXPRESS".equals(request.getShippingMethod());
		BigDecimal shipping = express ? EXPRESS_SHIPPING
				: (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : STANDARD_SHIPPING);
		BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
		BigDecimal grandTotal = subtotal.add(shipping).add(tax);

		Order order = Order.builder().user(user).status(OrderStatus.PLACED).totalAmount(grandTotal)
				.shippingAmount(shipping).taxAmount(tax)
				.shippingMethod(express ? "EXPRESS" : "STANDARD").shippingAddress(request.getShippingAddress())
				.build();

		orderItems.forEach(item -> item.setOrder(order));
		order.setItems(orderItems);

		Order saved = orderRepository.save(order);

		cart.getItems().clear();
		cartRepository.save(cart);

		orderEventPublisher.publishOrderStatusEvent(saved.getUser().getId(), saved.getId(), saved.getStatus().name());

		return toResponse(saved);
	}

	public Page<OrderResponse> getMyOrders(String email, Pageable pageable) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return orderRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
	}

	public OrderResponse getOrderById(Long orderId, String email, boolean isAdmin) {
		Order order = findByIdOrThrow(orderId);

		if (!isAdmin && !order.getUser().getEmail().equals(email)) {
			throw new ResourceNotFoundException("Order not found with id: " + orderId);
		}

		return toResponse(order);
	}

	public Page<OrderResponse> getAllOrders(Pageable pageable) {
		return orderRepository.findAll(pageable).map(this::toResponse);
	}

	@Transactional
	public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
		Order order = findByIdOrThrow(orderId);

		Set<OrderStatus> allowedNext = ALLOWED_TRANSITIONS.get(order.getStatus());
		if (!allowedNext.contains(newStatus)) {
			throw new BadRequestException("Cannot transition order from " + order.getStatus() + " to " + newStatus);
		}

		if (newStatus == OrderStatus.CANCELLED) {
			for (OrderItem item : order.getItems()) {
				inventoryService.restockForCancelledOrder(item.getProduct().getId(), item.getQuantity());
			}
		}

		order.setStatus(newStatus);

		orderEventPublisher.publishOrderStatusEvent(order.getUser().getId(), order.getId(), newStatus.name());
		Order saved = orderRepository.save(order);

		return toResponse(saved);
	}

	@Transactional
	public void cancelOrder(Long orderId, String email) {
		Order order = findByIdOrThrow(orderId);

		if (!order.getUser().getEmail().equals(email)) {
			throw new ResourceNotFoundException("Order not found with id: " + orderId);
		}

		updateStatus(orderId, OrderStatus.CANCELLED);
	}

	private Order findByIdOrThrow(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
	}

	private OrderResponse toResponse(Order order) {
		List<OrderItemResponse> itemResponses = order.getItems().stream()
				.map(item -> OrderItemResponse.builder().productId(item.getProduct().getId())
						.productName(item.getProduct().getName()).quantity(item.getQuantity())
						.unitPrice(item.getUnitPrice())
						.subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))).build())
				.toList();
		return OrderResponse.builder().id(order.getId()).status(order.getStatus()).totalAmount(order.getTotalAmount())
				.shippingAmount(order.getShippingAmount()).taxAmount(order.getTaxAmount())
				.shippingMethod(order.getShippingMethod()).shippingAddress(order.getShippingAddress())
				.items(itemResponses).createdAt(order.getCreatedAt()).build();
	}

}