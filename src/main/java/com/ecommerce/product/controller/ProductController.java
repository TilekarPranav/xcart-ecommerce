package com.ecommerce.product.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@PostMapping
	public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
		ProductResponse response = productService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response, "Product created successfully"));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id,
			@Valid @RequestBody ProductRequest request) {
		ProductResponse response = productService.update(id, request);
		return ResponseEntity.ok(ApiResponse.success(response, "Product updated successfully"));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		productService.delete(id);
		return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<Page<ProductResponse>>> search(@RequestParam(required = false) String name,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice, Pageable pageable) {
		Page<ProductResponse> results = productService.search(name, categoryId, minPrice, maxPrice, pageable);
		return ResponseEntity.ok(ApiResponse.success(results));
	}
}