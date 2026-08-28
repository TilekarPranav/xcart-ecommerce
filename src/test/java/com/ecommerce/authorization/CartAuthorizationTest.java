package com.ecommerce.authorization;

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
 * Controller-level authorization tests for Cart endpoints (GET, POST, PUT, DELETE).
 * Verifies that cart operations are strictly isolated per authenticated user.
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
		product = productRepository.save(Product.builder().name("Cart Test Product").description("desc")
				.price(new BigDecimal("15.50")).active(true).category(category).build());
		inventoryRepository.save(Inventory.builder().product(product).quantity(50).build());
	}

	@Test
	void cart_unauthenticatedRequest_returns401() throws Exception {
		mockMvc.perform(get("/cart")).andExpect(status().isUnauthorized());
	}

	@Test
	void cart_userCanAddAndRetrieveOwnCart() throws Exception {
		AuthedUser user = registerUser();
		Cookie csrf = fetchCsrfCookie();

		String addBody = """
				{"productId":%d,"quantity":2}
				""".formatted(product.getId());

		mockMvc.perform(post("/cart/add").cookie(user.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(addBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].quantity").value(2));

		mockMvc.perform(get("/cart").cookie(user.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].productId").value(product.getId()));
	}

	@Test
	void cart_isolationBetweenUsers() throws Exception {
		AuthedUser user1 = registerUser();
		AuthedUser user2 = registerUser();
		Cookie csrf = fetchCsrfCookie();

		// User 1 adds product
		String addBody = """
				{"productId":%d,"quantity":3}
				""".formatted(product.getId());
		mockMvc.perform(post("/cart/add").cookie(user1.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(addBody))
				.andExpect(status().isOk());

		// User 2 cart must be empty
		mockMvc.perform(get("/cart").cookie(user2.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void cart_otherUserCannotUpdateAnotherUsersCartItem() throws Exception {
		AuthedUser user1 = registerUser();
		AuthedUser user2 = registerUser();
		Cookie csrf = fetchCsrfCookie();

		// User 1 adds item
		String addBody = """
				{"productId":%d,"quantity":1}
				""".formatted(product.getId());
		String response = mockMvc.perform(post("/cart/add").cookie(user1.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(addBody))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		Long cartItemId = com.jayway.jsonpath.JsonPath.parse(response).read("$.data.items[0].cartItemId", Long.class);

		// User 2 tries to update User 1's cart item
		String updateBody = """
				{"cartItemId":%d,"quantity":5}
				""".formatted(cartItemId);
		mockMvc.perform(put("/cart/update").cookie(user2.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(updateBody))
				.andExpect(status().isNotFound());
	}

	@Test
	void cart_otherUserCannotDeleteAnotherUsersCartItem() throws Exception {
		AuthedUser user1 = registerUser();
		AuthedUser user2 = registerUser();
		Cookie csrf = fetchCsrfCookie();

		String addBody = """
				{"productId":%d,"quantity":1}
				""".formatted(product.getId());
		String response = mockMvc.perform(post("/cart/add").cookie(user1.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(addBody))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		Long cartItemId = com.jayway.jsonpath.JsonPath.parse(response).read("$.data.items[0].cartItemId", Long.class);

		// User 2 tries to delete User 1's cart item
		mockMvc.perform(delete("/cart/remove/" + cartItemId).cookie(user2.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isNotFound());
	}
}
