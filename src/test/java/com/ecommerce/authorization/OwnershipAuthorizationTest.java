package com.ecommerce.authorization;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ecommerce.category.entity.Category;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.repository.NotificationRepository;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.review.repository.ReviewRepository;
import com.ecommerce.support.AbstractIntegrationTest;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;

/**
 * Controller-level authorization tests for Order, Payment, Review, Notification,
 * and Admin Product endpoints.
 */
class OwnershipAuthorizationTest extends AbstractIntegrationTest {

	@Autowired
	private CategoryRepository categoryRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private InventoryRepository inventoryRepository;
	@Autowired
	private OrderRepository orderRepository;
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
	private ReviewRepository reviewRepository;
	@Autowired
	private NotificationRepository notificationRepository;

	private Product product;

	@BeforeEach
	void seedProduct() {
		Category category = categoryRepository.save(Category.builder().name("Cat-" + System.nanoTime()).build());
		product = productRepository.save(Product.builder().name("Owned Product").description("desc")
				.price(new BigDecimal("29.99")).active(true).category(category).build());
		inventoryRepository.save(Inventory.builder().product(product).quantity(50).build());
	}

	/** Places a real order for the given user via the actual HTTP flow (cart -> checkout). */
	private Long placeOrderFor(AuthedUser user) throws Exception {
		Cookie csrf = fetchCsrfCookie();
		String addBody = """
				{"productId":%d,"quantity":1}
				""".formatted(product.getId());
		mockMvc.perform(post("/cart/add").cookie(user.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(addBody))
				.andExpect(status().isOk());

		String orderBody = """
				{"shippingMethod":"STANDARD","shippingAddress":"1 Test St"}
				""";
		String response = mockMvc
				.perform(post("/orders").cookie(user.accessToken()).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
						.contentType(APPLICATION_JSON).content(orderBody))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		return com.jayway.jsonpath.JsonPath.parse(response).read("$.data.id", Long.class);
	}

	// ------------------------------- Order -------------------------------

	@Test
	void order_ownerCanViewTheirOwnOrder() throws Exception {
		AuthedUser owner = registerUser();
		Long orderId = placeOrderFor(owner);

		mockMvc.perform(get("/orders/" + orderId).cookie(owner.accessToken())).andExpect(status().isOk());
	}

	@Test
	void order_otherUserCannotViewSomeoneElsesOrder() throws Exception {
		AuthedUser owner = registerUser();
		AuthedUser attacker = registerUser();
		Long orderId = placeOrderFor(owner);

		mockMvc.perform(get("/orders/" + orderId).cookie(attacker.accessToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void order_otherUserCannotCancelSomeoneElsesOrder() throws Exception {
		AuthedUser owner = registerUser();
		AuthedUser attacker = registerUser();
		Long orderId = placeOrderFor(owner);
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(delete("/orders/" + orderId).cookie(attacker.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isNotFound());

		// and it must genuinely still be cancellable by its real owner afterwards
		mockMvc.perform(delete("/orders/" + orderId).cookie(owner.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk());
	}

	@Test
	void order_adminCanViewAnyUsersOrder() throws Exception {
		AuthedUser owner = registerUser();
		AuthedUser admin = registerAdminUser();
		Long orderId = placeOrderFor(owner);

		mockMvc.perform(get("/orders/" + orderId).cookie(admin.accessToken())).andExpect(status().isOk());
	}

	// ------------------------------ Payment -------------------------------

	@Test
	void payment_otherUserCannotPayForSomeoneElsesOrder() throws Exception {
		AuthedUser owner = registerUser();
		AuthedUser attacker = registerUser();
		Long orderId = placeOrderFor(owner);
		Cookie csrf = fetchCsrfCookie();
		String body = """
				{"orderId":%d}
				""".formatted(orderId);

		mockMvc.perform(post("/payments").cookie(attacker.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound());
	}

	@Test
	void payment_otherUserCannotViewSomeoneElsesPayment() throws Exception {
		AuthedUser owner = registerUser();
		AuthedUser attacker = registerUser();
		Long orderId = placeOrderFor(owner);
		Order order = orderRepository.findById(orderId).orElseThrow();
		paymentRepository.save(Payment.builder().order(order).status(PaymentStatus.SUCCESS)
				.amount(order.getTotalAmount()).providerRef("SEEDED-FOR-TEST").build());

		mockMvc.perform(get("/payments/" + orderId).cookie(attacker.accessToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void payment_ownerCanViewTheirOwnPayment() throws Exception {
		AuthedUser owner = registerUser();
		Long orderId = placeOrderFor(owner);
		Order order = orderRepository.findById(orderId).orElseThrow();
		paymentRepository.save(Payment.builder().order(order).status(PaymentStatus.SUCCESS)
				.amount(order.getTotalAmount()).providerRef("SEEDED-FOR-TEST").build());

		mockMvc.perform(get("/payments/" + orderId).cookie(owner.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("SUCCESS"));
	}

	@Test
	void payment_adminCanViewAnyUsersPayment() throws Exception {
		AuthedUser owner = registerUser();
		AuthedUser admin = registerAdminUser();
		Long orderId = placeOrderFor(owner);
		Order order = orderRepository.findById(orderId).orElseThrow();
		paymentRepository.save(Payment.builder().order(order).status(PaymentStatus.SUCCESS)
				.amount(order.getTotalAmount()).providerRef("SEEDED-FOR-TEST").build());

		mockMvc.perform(get("/payments/" + orderId).cookie(admin.accessToken())).andExpect(status().isOk());
	}

	// ------------------------------- Review -------------------------------

	@Test
	void review_otherUserCannotEditSomeoneElsesReview() throws Exception {
		AuthedUser author = registerUser();
		AuthedUser attacker = registerUser();
		Long reviewId = createReview(author);
		Cookie csrf = fetchCsrfCookie();
		String body = """
				{"rating":1,"comment":"edited by attacker"}
				""";

		mockMvc.perform(put("/reviews/" + reviewId).cookie(attacker.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isNotFound());
	}

	@Test
	void review_otherUserCannotDeleteSomeoneElsesReview() throws Exception {
		AuthedUser author = registerUser();
		AuthedUser attacker = registerUser();
		Long reviewId = createReview(author);
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(delete("/reviews/" + reviewId).cookie(attacker.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isNotFound());
	}

	@Test
	void review_adminCanDeleteAnyUsersReview() throws Exception {
		AuthedUser author = registerUser();
		AuthedUser admin = registerAdminUser();
		Long reviewId = createReview(author);
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(delete("/reviews/" + reviewId).cookie(admin.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk());
	}

	private Long createReview(AuthedUser author) throws Exception {
		Cookie csrf = fetchCsrfCookie();
		String body = """
				{"rating":5,"comment":"great product"}
				""";
		String response = mockMvc
				.perform(post("/products/" + product.getId() + "/reviews").cookie(author.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()).contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return com.jayway.jsonpath.JsonPath.parse(response).read("$.data.id", Long.class);
	}

	// ----------------------------- Notification ----------------------------

	@Test
	void notification_otherUserCannotMarkSomeoneElsesNotificationAsRead() throws Exception {
		AuthedUser owner = registerUser();
		AuthedUser attacker = registerUser();
		Long notificationId = seedNotificationFor(owner);
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(put("/notifications/" + notificationId + "/read").cookie(attacker.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isNotFound());
	}

	@Test
	void notification_ownerCanMarkTheirOwnNotificationAsRead() throws Exception {
		AuthedUser owner = registerUser();
		Long notificationId = seedNotificationFor(owner);
		Cookie csrf = fetchCsrfCookie();

		mockMvc.perform(put("/notifications/" + notificationId + "/read").cookie(owner.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk());
	}

	private Long seedNotificationFor(AuthedUser user) {
		User entity = userRepository.findByEmail(user.email()).orElseThrow();
		Notification notification = notificationRepository.save(Notification.builder().user(entity)
				.message("Test notification").read(false).createdAt(Instant.now()).build());
		return notification.getId();
	}

	// ------------------------- Admin: inactive product -----------------------

	@Test
	void adminProductEndpoint_returnsInactiveProduct_thatThePublicEndpointHides() throws Exception {
		AuthedUser admin = registerAdminUser();
		Cookie csrf = fetchCsrfCookie();

		// Soft-delete via the real admin flow (ProductService.delete sets active=false)
		mockMvc.perform(delete("/products/" + product.getId()).cookie(admin.accessToken()).cookie(csrf)
						.header("X-XSRF-TOKEN", csrf.getValue()))
				.andExpect(status().isOk());

		// Public endpoint must still 404
		mockMvc.perform(get("/products/" + product.getId())).andExpect(status().isNotFound());

		// New admin-only endpoint must be able to retrieve it
		mockMvc.perform(get("/admin/products/" + product.getId()).cookie(admin.accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.active").value(false));
	}

	@Test
	void adminProductEndpoint_rejectsNonAdminEntirely() throws Exception {
		AuthedUser regularUser = registerUser();

		mockMvc.perform(get("/admin/products/" + product.getId()).cookie(regularUser.accessToken()))
				.andExpect(status().isForbidden());
	}
}
