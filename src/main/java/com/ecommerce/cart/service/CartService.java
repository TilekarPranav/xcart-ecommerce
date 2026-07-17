package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final ProductRepository productRepository;
	private final InventoryRepository inventoryRepository;
	private final UserRepository userRepository;

	@Transactional
	public CartResponse getCart(String email) {
		Cart cart = findOrCreateCart(email);
		return toResponse(cart);
	}

	@Transactional
	public CartResponse addItem(String email, AddCartItemRequest request) {
		Cart cart = findOrCreateCart(email);
		Product product = productRepository.findById(request.getProductId()).orElseThrow(
				() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

		checkStockAvailable(product.getId(), request.getQuantity());

		CartItem existingItem = cart.getItems().stream()
				.filter(item -> item.getProduct().getId().equals(product.getId())).findFirst().orElse(null);

		if (existingItem != null) {
			existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
		} else {
			CartItem newItem = CartItem.builder().cart(cart).product(product).quantity(request.getQuantity())
					.unitPriceSnapshot(product.getPrice()).build();
			cart.getItems().add(newItem);
		}

		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	@Transactional
	public CartResponse updateItemQuantity(String email, UpdateCartItemRequest request) {
		Cart cart = findOrCreateCart(email);

		CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(request.getCartItemId())).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

		checkStockAvailable(item.getProduct().getId(), request.getQuantity());
		item.setQuantity(request.getQuantity());

		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	@Transactional
	public CartResponse removeItem(String email, Long cartItemId) {
		Cart cart = findOrCreateCart(email);
		boolean removed = cart.getItems().removeIf(item -> item.getId().equals(cartItemId));

		if (!removed) {
			throw new ResourceNotFoundException("Cart item not found");
		}

		Cart saved = cartRepository.save(cart);
		return toResponse(saved);
	}

	@Transactional
	public void clearCart(String email) {
		Cart cart = findOrCreateCart(email);
		cart.getItems().clear();
		cartRepository.save(cart);
	}

	private void checkStockAvailable(Long productId, int requestedQuantity) {
		int available = inventoryRepository.findByProductId(productId).map(inv -> inv.getQuantity()).orElse(0);

		if (requestedQuantity > available) {
			throw new BadRequestException("Insufficient stock - only " + available + " available");
		}
	}

	private Cart findOrCreateCart(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return cartRepository.findByUserId(user.getId())
				.orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
	}

	private CartResponse toResponse(Cart cart) {
		List<CartItemResponse> itemResponses = cart.getItems().stream()
				.map(item -> CartItemResponse.builder().cartItemId(item.getId()).productId(item.getProduct().getId())
						.productName(item.getProduct().getName()).unitPrice(item.getUnitPriceSnapshot())
						.quantity(item.getQuantity())
						.subtotal(item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity()))).build())
				.toList();

		BigDecimal total = itemResponses.stream().map(CartItemResponse::getSubtotal).reduce(BigDecimal.ZERO,
				BigDecimal::add);

		return CartResponse.builder().cartId(cart.getId()).items(itemResponses).totalAmount(total).build();
	}
}