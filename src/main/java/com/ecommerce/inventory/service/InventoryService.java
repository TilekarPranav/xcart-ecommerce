package com.ecommerce.inventory.service;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

	private final InventoryRepository inventoryRepository;

	private static final int MAX_ATTEMPTS = 3;
	private static final long RETRY_DELAY_MS = 25L;

	public InventoryResponse getByProductId(Long id) {
		Inventory inventory = findByProductIdOrThrow(id);
		return toResponse(inventory);
	}

	@Transactional
	public InventoryResponse updateStock(InventoryUpdateRequest request) {
		int attempt = 0;
		while (true) {
			try {
				return doUpdateStock(request);
			} catch (OptimisticLockingFailureException ex) {
				attempt++;
				if (attempt >= MAX_ATTEMPTS) {
					throw new BadRequestException(
							"Could not update stock due to a conflicting update - please try again");
				}
				backoff(attempt);
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
		// saveAndFlush, not save: save() alone defers the actual UPDATE (and its
		// version check) to transaction commit, which happens outside this
		// try/catch entirely. Flushing here forces the optimistic-lock check to
		// happen synchronously, where the retry loop can actually see it.
		Inventory saved = inventoryRepository.saveAndFlush(inventory);
		return toResponse(saved);
	}

	@Transactional
	public void decreaseStockForOrder(Long productId, int amount) {
		int attempt = 0;
		while (true) {
			try {
				doDecreaseStockForOrder(productId, amount);
				return;
			} catch (OptimisticLockingFailureException ex) {
				attempt++;
				if (attempt >= MAX_ATTEMPTS) {
					throw new ConflictException(
							"This item just sold out or is being purchased by someone else — please try again");
				}
				backoff(attempt);
			}
		}
	}

	private void doDecreaseStockForOrder(Long productId, int amount) {
		Inventory inventory = findByProductIdOrThrow(productId);
		int newQuantity = inventory.getQuantity() - amount;

		if (newQuantity < 0) {
			throw new BadRequestException("Insufficient stock for product id: " + productId);
		}

		inventory.setQuantity(newQuantity);
		inventoryRepository.saveAndFlush(inventory);
	}

	@Transactional
	public void restockForCancelledOrder(Long productId, int amount) {
		Inventory inventory = findByProductIdOrThrow(productId);
		inventory.setQuantity(inventory.getQuantity() + amount);
		inventoryRepository.save(inventory);
	}

	/** Small fixed delay, not full exponential backoff — this only needs to
	 *  avoid an instant tight-loop retry against the same contended row, not
	 *  survive sustained high contention. */
	private void backoff(int attempt) {
		try {
			Thread.sleep(RETRY_DELAY_MS * attempt);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	private Inventory findByProductIdOrThrow(Long productId) {
		return inventoryRepository.findByProductId(productId).orElseThrow(
				() -> new ResourceNotFoundException("No inventory record found for product id:" + productId));
	}

	private InventoryResponse toResponse(Inventory inventory) {
		return InventoryResponse.builder().productId(inventory.getProduct().getId())
				.productName(inventory.getProduct().getName()).quantity(inventory.getQuantity())
				.inStock(inventory.getQuantity() > 0).build();
	}
}