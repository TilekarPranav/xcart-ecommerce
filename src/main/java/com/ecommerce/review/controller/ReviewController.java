package com.ecommerce.review.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.dto.ReviewResponse;
import com.ecommerce.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping("/products/{productId}/reviews")
	public ResponseEntity<ApiResponse<ReviewResponse>> create(@PathVariable Long productId,
			Authentication authentication, @Valid @RequestBody ReviewRequest request) {
		ReviewResponse response = reviewService.create(productId, authentication.getName(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Review submitted"));
	}

	@GetMapping("/products/{productId}/reviews")
	public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getByProduct(@PathVariable Long productId,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(reviewService.getByProductId(productId, pageable)));
	}

	@GetMapping("/products/{productId}/reviews/average")
	public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long productId) {
		return ResponseEntity.ok(ApiResponse.success(reviewService.getAverageRating(productId)));
	}

	@PutMapping("/reviews/{id}")
	public ResponseEntity<ApiResponse<ReviewResponse>> update(@PathVariable Long id, Authentication authentication,
			@Valid @RequestBody ReviewRequest request) {
		ReviewResponse response = reviewService.update(id, authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.success(response, "Review updated"));
	}

	@DeleteMapping("/reviews/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication authentication) {
		boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		reviewService.delete(id, authentication.getName(), isAdmin);
		return ResponseEntity.ok(ApiResponse.success(null, "Review deleted"));
	}
}