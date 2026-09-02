package com.ecommerce.inventory.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.ecommerce.exception.ConflictException;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;

@ExtendWith(MockitoExtension.class)
class InventoryOptimisticLockTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .build();
    }

    @Test
    void decreaseStockForOrder_retrySucceedsAfterInitialConflict() {
        when(inventoryRepository.findByProductId(1L))
                .thenAnswer(inv -> Optional.of(
                        Inventory.builder()
                                .id(1L)
                                .product(product)
                                .quantity(10)
                                .version(0L)
                                .build()));

        when(inventoryRepository.saveAndFlush(any(Inventory.class)))
                .thenThrow(new OptimisticLockingFailureException(
                        "Simulated conflict on attempt 1"))
                .thenAnswer(inv -> inv.getArgument(0));

        inventoryService.decreaseStockForOrder(1L, 2);

        verify(inventoryRepository, times(2))
                .saveAndFlush(any(Inventory.class));
    }

    @Test
    void decreaseStockForOrder_exhaustsRetries_throwsConflictException() {
        when(inventoryRepository.findByProductId(1L))
                .thenAnswer(inv -> Optional.of(
                        Inventory.builder()
                                .id(1L)
                                .product(product)
                                .quantity(10)
                                .version(0L)
                                .build()));

        when(inventoryRepository.saveAndFlush(any(Inventory.class)))
                .thenThrow(new OptimisticLockingFailureException(
                        "Simulated conflict"));

        assertThatThrownBy(() ->
                inventoryService.decreaseStockForOrder(1L, 2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(
                        "sold out or is being purchased");

        verify(inventoryRepository, times(3))
                .saveAndFlush(any(Inventory.class));
    }
}