package com.ecommerce.support;

import static org.junit.jupiter.api.Assertions.fail;
<<<<<<< HEAD
=======
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
>>>>>>> 7a59d717989bba0c7ca693d36abca14f4438bcce

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
<<<<<<< HEAD
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
=======
>>>>>>> 7a59d717989bba0c7ca693d36abca14f4438bcce
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Shared setup for the new controller-level security/authorization tests added during
 * the CSRF / token-type / IDOR remediation pass.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

	@Autowired
	protected WebApplicationContext webApplicationContext;

	protected MockMvc mockMvc;

<<<<<<< HEAD
=======
	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(springSecurity())
				.build();
	}

>>>>>>> 7a59d717989bba0c7ca693d36abca14f4438bcce
	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected RoleRepository roleRepository;

	@MockitoBean
	protected KafkaTemplate<String, Object> kafkaTemplate;

<<<<<<< HEAD
	@BeforeEach
	void initMockMvc() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
		ensureRolesExist();
	}

	private void ensureRolesExist() {
		roleRepository.findByName(Role.ROLE_CUSTOMER)
				.orElseGet(() -> roleRepository.save(Role.builder().name(Role.ROLE_CUSTOMER).build()));
		roleRepository.findByName(Role.ROLE_ADMIN)
				.orElseGet(() -> roleRepository.save(Role.builder().name(Role.ROLE_ADMIN).build()));
	}

=======
>>>>>>> 7a59d717989bba0c7ca693d36abca14f4438bcce
	/** A registered user plus the cookies the backend actually issued at registration. */
	public record AuthedUser(String email, String password, Cookie accessToken, Cookie refreshToken) {
	}

	protected AuthedUser registerUser() throws Exception {
		String email = "user-" + UUID.randomUUID() + "@test.com";
		String password = "Password123!";
		String body = """
				{"name":"Test User","email":"%s","password":"%s"}
				""".formatted(email, password);

		MvcResult result = mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content(body))
				.andReturn();

		Cookie access = result.getResponse().getCookie("accessToken");
		Cookie refresh = result.getResponse().getCookie("refreshToken");
		if (access == null || refresh == null) {
			fail("Registration did not set both auth cookies — response: " + result.getResponse().getContentAsString());
		}
		return new AuthedUser(email, password, access, refresh);
	}

	protected AuthedUser registerAdminUser() throws Exception {
		AuthedUser user = registerUser();
		User entity = userRepository.findByEmail(user.email())
				.orElseThrow(() -> new IllegalStateException("registered user not found: " + user.email()));
		Role admin = roleRepository.findByName(Role.ROLE_ADMIN)
<<<<<<< HEAD
				.orElseGet(() -> roleRepository.save(new Role(null, Role.ROLE_ADMIN)));
=======
				.orElseThrow(() -> new IllegalStateException(
						"ROLE_ADMIN not seeded — DataInitializer should have created it on startup"));
>>>>>>> 7a59d717989bba0c7ca693d36abca14f4438bcce
		entity.setRoles(Set.of(admin));
		userRepository.save(entity);

		String body = """
				{"email":"%s","password":"%s"}
				""".formatted(user.email(), user.password());
		MvcResult result = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body))
				.andReturn();
		Cookie access = result.getResponse().getCookie("accessToken");
		Cookie refresh = result.getResponse().getCookie("refreshToken");
		return new AuthedUser(user.email(), user.password(), access, refresh);
	}

	protected Cookie fetchCsrfCookie() throws Exception {
		MvcResult result = mockMvc.perform(get("/auth/me")).andReturn();
		Cookie csrf = result.getResponse().getCookie("XSRF-TOKEN");
		if (csrf == null) {
			fail("CsrfCookieFilter did not issue an XSRF-TOKEN cookie on a plain GET request");
		}
		return csrf;
	}
}
