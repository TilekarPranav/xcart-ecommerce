package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddCartItemRequest;
import com.ecommerce.cart.dto.CartItemResponse;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
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
class CartServiceTest {

	@Mock
	private CartRepository cartRepository;
	@Mock
	private ProductRepository productRepository;
	@Mock
	private InventoryRepository inventoryRepository;
	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CartService cartService;

	private User user;
	private Product product;
	private Cart cart;
	private CartItem cartItem;
	private Inventory inventory;

	@BeforeEach
	void setUp() {
		user = User.builder().id(1L).name("Pranav").email("[pranav@test.com](mailto:pranav@test.com)").build();
		product = Product.builder().id(10L).name("Laptop").price(new BigDecimal("999.99")).build();
		cart = Cart.builder().id(1L).user(user).build();
		cartItem = CartItem.builder().id(1L).cart(cart).product(product).quantity(1)
				.unitPriceSnapshot(new BigDecimal("999.99")).build();
		cart.getItems().add(cartItem);
		inventory = Inventory.builder().id(1L).product(product).quantity(5).build();
	}

	@Test
	void getCart_whenCartExists_returnsCartWithItems() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		CartResponse response = cartService.getCart("[pranav@test.com](mailto:pranav@test.com)");
		assertThat(response.getCartId()).isEqualTo(1L);
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getProductName()).isEqualTo("Laptop");
		assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("999.99"));
	}

	@Test
	void getCart_whenNoCartExists_createsNewCart() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
		when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> {
			Cart c = inv.getArgument(0); c.setId(1L); return c;
		});
		CartResponse response = cartService.getCart("[pranav@test.com](mailto:pranav@test.com)");
		assertThat(response.getCartId()).isEqualTo(1L);
		assertThat(response.getItems()).isEmpty();
		assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getCart_userNotFound_throwsResourceNotFoundException() {
		when(userRepository.findByEmail("[ghost@test.com](mailto:ghost@test.com)")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> cartService.getCart("[ghost@test.com](mailto:ghost@test.com)"))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("User not found");
	}

	@Test
	void addItem_newProduct_addsToCartWithPriceSnapshot() {
		Product newProduct = Product.builder().id(20L).name("Mouse").price(new BigDecimal("29.99")).build();
		Inventory mouseInventory = Inventory.builder().id(2L).product(newProduct).quantity(10).build();
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(20L)).thenReturn(Optional.of(newProduct));
		when(inventoryRepository.findByProductId(20L)).thenReturn(Optional.of(mouseInventory));
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);

		CartResponse response = cartService.addItem("[pranav@test.com](mailto:pranav@test.com)", new AddCartItemRequest(20L, 2));
		assertThat(response.getItems()).hasSize(2);
		CartItemResponse newItem = response.getItems().stream()
				.filter(i -> i.getProductId().equals(20L)).findFirst().orElseThrow();
		assertThat(newItem.getUnitPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
		assertThat(newItem.getQuantity()).isEqualTo(2);
	}

	@Test
	void addItem_existingProduct_increasesQuantity() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(10L)).thenReturn(Optional.of(product));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);

		CartResponse response = cartService.addItem("[pranav@test.com](mailto:pranav@test.com)", new AddCartItemRequest(10L, 2));
		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
		assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
	}

	@Test
	void addItem_insufficientStock_throwsBadRequestException() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(10L)).thenReturn(Optional.of(product));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
		assertThatThrownBy(() -> cartService.addItem("[pranav@test.com](mailto:pranav@test.com)", new AddCartItemRequest(10L, 100)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("Insufficient stock");
		verify(cartRepository, never()).save(any());
	}

	@Test
	void addItem_productNotFound_throwsResourceNotFoundException() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(productRepository.findById(999L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> cartService.addItem("[pranav@test.com](mailto:pranav@test.com)", new AddCartItemRequest(999L, 1)))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Product not found");
	}

	@Test
	void updateItemQuantity_validItem_updatesQuantity() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);
		CartResponse response = cartService.updateItemQuantity("[pranav@test.com](mailto:pranav@test.com)", new UpdateCartItemRequest(1L, 3));
		assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
	}

	@Test
	void updateItemQuantity_cartItemNotFound_throwsResourceNotFoundException() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		assertThatThrownBy(() -> cartService.updateItemQuantity("[pranav@test.com](mailto:pranav@test.com)", new UpdateCartItemRequest(999L, 1)))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Cart item not found");
	}

	@Test
	void updateItemQuantity_quantityExceedsStock_throwsBadRequestException() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory));
		assertThatThrownBy(() -> cartService.updateItemQuantity("[pranav@test.com](mailto:pranav@test.com)", new UpdateCartItemRequest(1L, 50)))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("Insufficient stock");
	}

	@Test
	void removeItem_validItem_removesFromCart() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
		CartResponse response = cartService.removeItem("[pranav@test.com](mailto:pranav@test.com)", 1L);
		assertThat(response.getItems()).isEmpty();
	}

	@Test
	void removeItem_itemNotFound_throwsResourceNotFoundException() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		assertThatThrownBy(() -> cartService.removeItem("[pranav@test.com](mailto:pranav@test.com)", 999L))
				.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Cart item not found");
	}

	@Test
	void clearCart_removesAllItems() {
		when(userRepository.findByEmail("[pranav@test.com](mailto:pranav@test.com)")).thenReturn(Optional.of(user));
		when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
		when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
		cartService.clearCart("[pranav@test.com](mailto:pranav@test.com)");
		assertThat(cart.getItems()).isEmpty();
		verify(cartRepository).save(any(Cart.class));
	}
}
