package com.ecommerce.product.service;

import com.ecommerce.category.entity.Category;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSpecifications;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final InventoryRepository inventoryRepository;

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Category category = findCategoryOrThrow(request.getCategoryId());

		Product product = Product.builder().name(request.getName()).description(request.getDescription())
				.price(request.getPrice()).imageUrl(request.getImageUrl()).category(category).active(true).build();

		Product saved = productRepository.save(product);

		Inventory inventory = Inventory.builder().product(saved).quantity(0).build();
		inventoryRepository.save(inventory);

		return toResponse(saved);
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = findByIdOrThrow(id);
		Category category = findCategoryOrThrow(request.getCategoryId());

		product.setName(request.getName());
		product.setDescription(request.getDescription());
		product.setPrice(request.getPrice());
		product.setImageUrl(request.getImageUrl());
		product.setCategory(category);

		Product saved = productRepository.save(product);
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long id) {
		Product product = findByIdOrThrow(id);
		product.setActive(false);
		productRepository.save(product);
	}

	public ProductResponse getById(Long id) {
		return toResponse(findByIdOrThrow(id));
	}

	public Page<ProductResponse> search(String name, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
			Pageable pageable) {

		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new BadRequestException("minPrice cannot be greater than maxPrice");
		}

		var spec = ProductSpecifications.build(name, categoryId, minPrice, maxPrice);
		return productRepository.findAll(spec, pageable).map(this::toResponse);
	}

	private Product findByIdOrThrow(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
	}

	private Category findCategoryOrThrow(Long categoryId) {
		return categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
	}

	private ProductResponse toResponse(Product product) {
		return ProductResponse.builder().id(product.getId()).name(product.getName())
				.description(product.getDescription()).price(product.getPrice()).imageUrl(product.getImageUrl())
				.active(product.isActive()).categoryId(product.getCategory().getId())
				.categoryName(product.getCategory().getName()).createdAt(product.getCreatedAt()).build();
	}
}