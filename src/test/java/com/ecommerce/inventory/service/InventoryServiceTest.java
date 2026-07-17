package com.ecommerce.inventory.service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

	@Mock
	private InventoryRepository inventoryRepository;

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private InventoryService inventoryService;

	private Product product;
	private Inventory inventory;

	@BeforeEach
	void setUp() {
		product = Product.builder().id(1L).name("Test Product").build();
		inventory = Inventory.builder().id(1L).product(product).quantity(10).version(0L).build();
	}

	@Test
	void getByProductId_whenExists_returnsInventoryResponse() {
		// Arrange: tell the fake repository what to return when asked
		when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

		// Act: call the real method under test
		InventoryResponse response = inventoryService.getByProductId(1L);

		// Assert: check the result is what we expect
		assertThat(response.getQuantity()).isEqualTo(10);
		assertThat(response.isInStock()).isTrue();
	}

	@Test
	void getByProductId_whenNotFound_throwsResourceNotFoundException() {
		when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> inventoryService.getByProductId(99L)).isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void updateStock_reduceOperation_decreasesQuantityCorrectly() {
		InventoryUpdateRequest request = new InventoryUpdateRequest(1L, 3, "REDUCE");

		when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
		when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InventoryResponse response = inventoryService.updateStock(request);

		assertThat(response.getQuantity()).isEqualTo(7); // 10 - 3
		verify(inventoryRepository, times(1)).save(any(Inventory.class));
	}

	@Test
	void updateStock_reduceBelowZero_throwsBadRequestException() {
		InventoryUpdateRequest request = new InventoryUpdateRequest(1L, 50, "REDUCE");

		when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

		assertThatThrownBy(() -> inventoryService.updateStock(request)).isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Insufficient stock");

		// Critically: save should NEVER be called if validation fails
		verify(inventoryRepository, never()).save(any(Inventory.class));
	}

	@Test
	void updateStock_invalidOperation_throwsBadRequestException() {
		InventoryUpdateRequest request = new InventoryUpdateRequest(1L, 5, "MULTIPLY");

		when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

		assertThatThrownBy(() -> inventoryService.updateStock(request)).isInstanceOf(BadRequestException.class)
				.hasMessageContaining("SET, ADD, or REDUCE");
	}
}