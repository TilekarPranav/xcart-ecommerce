package com.ecommerce.category.controller;

import com.ecommerce.category.dto.CategoryRequest;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.service.CategoryService;
import com.ecommerce.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@PostMapping("/categories")
	public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
		CategoryResponse response = categoryService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response, "Category created successfully"));
	}

	@PutMapping("/categories/{id}")
	public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
			@Valid @RequestBody CategoryRequest request) {
		CategoryResponse response = categoryService.update(id, request);
		return ResponseEntity.ok(ApiResponse.success(response, "Category updated successfully"));
	}

	@DeleteMapping("/categories/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		categoryService.delete(id);
		return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
	}

	@GetMapping("/categories")
	public ResponseEntity<ApiResponse<List<CategoryResponse>>> listAll() {
		List<CategoryResponse> categories = categoryService.listAll();
		return ResponseEntity.ok(ApiResponse.success(categories));
	}
}