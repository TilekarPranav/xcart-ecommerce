package com.ecommerce.admin.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only product lookups that the public ProductController deliberately can't
 * provide. Authorization is enforced by SecurityConfig's existing "/admin/**" ->
 * hasRole("ADMIN") catch-all — no new security rule needed for this endpoint.
 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private final ProductService productService;

	/**
	 * Unlike GET /products/{id} (ProductController), this does not 404 on a
	 * deactivated product — the admin product-management UI needs to be able to load
	 * a soft-deleted product to view/edit/reactivate it.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ProductResponse>> getByIdIncludingInactive(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success(productService.getByIdForAdmin(id)));
	}
}
