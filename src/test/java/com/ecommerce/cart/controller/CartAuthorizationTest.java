package com.ecommerce.cart.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ecommerce.category.entity.Category;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.support.AbstractIntegrationTest;

import jakarta.servlet.http.Cookie;

/**
 * Controller-level authorization and isolation tests for Cart endpoints.
 */
class CartAuthorizationTest extends AbstractIntegrationTest {

	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private InventoryRepository inventoryRepository;

	private Product product;

	@BeforeEach
	void seedProduct() {
		Category category = categoryRepository.save(Category.builder().name("CartCat-" + System.nanoTime()).build());
		product = productRepository.save(Product.builder().name("Cart Product").description("desc")
				.price(new BigDecimal("15.50")).active(true).category(category).build());
		inventoryRepository.save(Inventory.builder().product(product).quantity(100).build());
	}

	@Test
	void getCart_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/cart")).andExpect(status().isUnauthorized());
	}

	@Test
	void cart_userCartIsIsolatedFromOtherUsers() throws Exception {
		AuthedUser userA = registerUser();
		AuthedUser userB = registerUser();
		Cookie csrfA = fetchCsrfCookie();

		String addBody = """
				{"productId":%d,"quantity":2}
				""".formatted(product.getId());

		mockMvc.perform(post("/cart/add").cookie(userA.accessToken()).cookie(csrfA)
						.header("X-XSRF-TOKEN", csrfA.getValue()).contentType(APPLICATION_JSON).content(addBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].productId").value(product.getId()))
				.andExpect(jsonPath("$.data.items[0].quantity").value(2));

		// User B's cart must remain empty
		mockMvc.perform(get("/cart").cookie(userB.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void cart_cannotRemoveAnotherUsersCartItem() throws Exception {
		AuthedUser userA = registerUser();
		AuthedUser userB = registerUser();
		Cookie csrfA = fetchCsrfCookie();
		Cookie csrfB = fetchCsrfCookie();

		String addBody = """
				{"productId":%d,"quantity":1}
				""".formatted(product.getId());

		String response = mockMvc.perform(post("/cart/add").cookie(userA.accessToken()).cookie(csrfA)
						.header("X-XSRF-TOKEN", csrfA.getValue()).contentType(APPLICATION_JSON).content(addBody))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		Long cartItemId = com.jayway.jsonpath.JsonPath.parse(response).read("$.data.items[0].cartItemId", Long.class);

		// User B tries to remove User A's cart item -> 404
		mockMvc.perform(delete("/cart/remove/" + cartItemId).cookie(userB.accessToken()).cookie(csrfB)
						.header("X-XSRF-TOKEN", csrfB.getValue()))
				.andExpect(status().isNotFound());
	}
}
