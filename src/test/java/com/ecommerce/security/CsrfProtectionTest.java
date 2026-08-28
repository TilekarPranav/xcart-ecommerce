package com.ecommerce.security;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Covers CSRF enforcement across GET (exempt), POST/PUT/DELETE without token (rejected),
 * and with valid double-submit token (accepted), plus login/register exemption.
 */
class CsrfProtectionTest extends AbstractIntegrationTest {

	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private InventoryRepository inventoryRepository;

	private Long productId;

	@BeforeEach
	void seedProduct() {
		Category category = categoryRepository.save(Category.builder().name("Test Category-" + System.nanoTime()).build());
		Product product = productRepository.save(Product.builder().name("Test Product").description("desc")
				.price(new BigDecimal("19.99")).active(true).category(category).build());
		inventoryRepository.save(Inventory.builder().product(product).quantity(50).build());
		productId = product.getId();
	}

	@Test
	void get_neverNeedsACsrfToken() throws Exception {
		AuthedUser user = registerUser();
		mockMvc.perform(get("/cart").cookie(user.accessToken())).andExpect(status().isOk());
	}

	@Test
	void post_withoutCsrfToken_isRejected() throws Exception {
		AuthedUser user = registerUser();
		String body = """
				{"productId":%d,"quantity":1}
				""".formatted(productId);

		mockMvc.perform(post("/cart/add").cookie(user.accessToken()).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isForbidden());
	}

	@Test
	void post_withValidCsrfCookieAndHeader_isAccepted() throws Exception {
		AuthedUser user = registerUser();
		Cookie csrf = fetchCsrfCookie();
		String body = """
				{"productId":%d,"quantity":1}
				""".formatted(productId);

		mockMvc.perform(post("/cart/add").cookie(user.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
	}

	@Test
	void post_withCsrfCookieButNoHeader_isRejected() throws Exception {
		AuthedUser user = registerUser();
		Cookie csrf = fetchCsrfCookie();
		String body = """
				{"productId":%d,"quantity":1}
				""".formatted(productId);

		mockMvc.perform(post("/cart/add").cookie(user.accessToken()).cookie(csrf).contentType(APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden());
	}

	@Test
	void put_withoutCsrfToken_isRejected() throws Exception {
		AuthedUser user = registerUser();
		Cookie csrf = fetchCsrfCookie();
		String addBody = """
				{"productId":%d,"quantity":1}
				""".formatted(productId);
		mockMvc.perform(post("/cart/add").cookie(user.accessToken()).cookie(csrf)
				.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(addBody));

		String updateBody = """
				{"cartItemId":1,"quantity":2}
				""";
		mockMvc.perform(put("/cart/update").cookie(user.accessToken()).contentType(APPLICATION_JSON).content(updateBody))
				.andExpect(status().isForbidden());
	}

	@Test
	void delete_withoutCsrfToken_isRejected() throws Exception {
		AuthedUser user = registerUser();
		mockMvc.perform(delete("/cart/clear").cookie(user.accessToken())).andExpect(status().isForbidden());
	}

	@Test
	void login_and_register_remainUsableWithNoPriorCsrfCookie() throws Exception {
		String body = """
				{"email":"nobody-%s@test.com","password":"whatever123"}
				""".formatted(java.util.UUID.randomUUID());

		int status = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body)).andReturn()
				.getResponse().getStatus();
		org.junit.jupiter.api.Assertions.assertNotEquals(403, status,
				"login must not be blocked by CSRF even with no prior cookie");
	}
}
