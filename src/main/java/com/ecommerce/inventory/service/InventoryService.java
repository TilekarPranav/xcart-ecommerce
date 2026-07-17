package com.ecommerce.inventory.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

	private final InventoryRepository inventoryRepository;
	private final ProductRepository productRepository;

	public InventoryResponse getByProductId(Long id) {
		Inventory inventory = findByProductIdOrThrow(id);
		return toResponse(inventory);
	}

	@Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 3)
	@Transactional
	public InventoryResponse updateStock(InventoryUpdateRequest request) {
		int maxAttempts = 3;
		int attempt = 0;

		while (true) {
			try {
				return doUpdateStock(request);
			} catch (OptimisticLockingFailureException ex) {
				attempt++;
				if (attempt >= maxAttempts) {
					throw new BadRequestException(
							"Could not update stock due to a conflicting update - please try again");
				}
			}
		}
	}

	private InventoryResponse doUpdateStock(InventoryUpdateRequest request) {
		Inventory inventory = findByProductIdOrThrow(request.getProductId());

		int newQuantity = switch (request.getOperation().toUpperCase()) {
		case "SET" -> request.getQuantity();
		case "ADD" -> inventory.getQuantity() + request.getQuantity();
		case "REDUCE" -> inventory.getQuantity() - request.getQuantity();
		default -> throw new BadRequestException("Operation must be SET, ADD, or REDUCE");
		};

		if (newQuantity < 0) {
			throw new BadRequestException("Insufficient stock - cannot reduce below zero");
		}

		inventory.setQuantity(newQuantity);
		Inventory saved = inventoryRepository.save(inventory);
		return toResponse(saved);
	}

	@Transactional
	public void decreaseStockForOrder(Long productId, int amount) {
		Inventory inventory = findByProductIdOrThrow(productId);
		int newQuantity = inventory.getQuantity() - amount;

		if (newQuantity < 0) {
			throw new BadRequestException("Insufficient stock for product id: " + productId);
		}

		inventory.setQuantity(newQuantity);
		inventoryRepository.save(inventory);
	}

	@Transactional
	public void restockForCancelledOrder(Long productId, int amount) {
		Inventory inventory = findByProductIdOrThrow(productId);
		inventory.setQuantity(inventory.getQuantity() + amount);
		inventoryRepository.save(inventory);
	}

	private Inventory findByProductIdOrThrow(Long productId) {
		return inventoryRepository.findByProductId(productId).orElseThrow(
				() -> new ResourceNotFoundException("No inventory record found for product id:" + productId));
	}

	private Product findProductOrThrow(Long productId) {
		return productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
	}

	private InventoryResponse toResponse(Inventory inventory) {
		return InventoryResponse.builder().productId(inventory.getProduct().getId())
				.productName(inventory.getProduct().getName()).quantity(inventory.getQuantity())
				.inStock(inventory.getQuantity() > 0).build();
	}
}
