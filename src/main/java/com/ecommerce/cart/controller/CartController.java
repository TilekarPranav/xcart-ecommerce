package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping
	public ResponseEntity<ApiResponse<CartResponse>> getCart(Authentication authentication) {
		return ResponseEntity.ok(ApiResponse.success(cartService.getCart(authentication.getName())));
	}

	@PostMapping("/add")
	public ResponseEntity<ApiResponse<CartResponse>> addItem(Authentication authentication,
			@Valid @RequestBody AddCartItemRequest request) {
		CartResponse response = cartService.addItem(authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.success(response, "Item added to cart"));
	}

	@PutMapping("/update")
	public ResponseEntity<ApiResponse<CartResponse>> updateItem(Authentication authentication,
			@Valid @RequestBody UpdateCartItemRequest request) {
		CartResponse response = cartService.updateItemQuantity(authentication.getName(), request);
		return ResponseEntity.ok(ApiResponse.success(response, "Cart updated"));
	}

	@DeleteMapping("/remove/{cartItemId}")
	public ResponseEntity<ApiResponse<CartResponse>> removeItem(Authentication authentication,
			@PathVariable Long cartItemId) {
		CartResponse response = cartService.removeItem(authentication.getName(), cartItemId);
		return ResponseEntity.ok(ApiResponse.success(response, "Item removed from cart"));
	}

	@DeleteMapping("/clear")
	public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
		cartService.clearCart(authentication.getName());
		return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared"));
	}
}