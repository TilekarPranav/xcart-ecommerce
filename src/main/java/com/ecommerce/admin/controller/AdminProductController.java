package com.ecommerce.admin.controller;

import java.math.BigDecimal;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only product lookups that the public ProductController deliberately can't
 * provide. Authorization is enforced by SecurityConfig's existing "/admin/**" ->
 * hasRole("ADMIN") catch-all — no new security rule needed for any endpoint here.
 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private final ProductService productService;

	/**
	 * Unlike GET /products/{id} (ProductController), this does not 404 on a
	 * deactivated product — the admin product-management UI needs to be able to
	 * load a soft-deleted product to view/edit/reactivate it.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> getByIdIncludingInactive(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success(productService.getByIdForAdmin(id)));
	}

	/**
	 * Backs the admin product-management table. Unlike GET /products/search
	 * (ProductController), this includes deactivated products — otherwise a
	 * deactivated product is unreachable from the admin UI even though
	 * getByIdIncludingInactive() above can technically return it.
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<Page<ProductResponse>>> search(@RequestParam(required = false) String name,
			@RequestParam(required = false) Long categoryId, @RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice, Pageable pageable) {
		return ResponseEntity
				.ok(ApiResponse.success(productService.searchForAdmin(name, categoryId, minPrice, maxPrice, pageable)));
	}

	/** The only way any product goes from inactive back to active. */
	@PutMapping("/{id}/reactivate")
	public ResponseEntity<ApiResponse<ProductResponse>> reactivate(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success(productService.reactivate(id), "Product reactivated"));
	}
}