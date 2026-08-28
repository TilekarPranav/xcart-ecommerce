package com.ecommerce.config;

import java.io.IOException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.JwtAuthenticationFilter;
import com.ecommerce.security.PartitionedCookieCsrfTokenRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomUserDetailsService userDetailsService;
	private final JwtAuthenticationFilter authenticationFilter;

	private static final String[] PUBLIC_ENDPOINTS = { "/auth/register", "/auth/login", "/auth/refresh",
			"/auth/logout", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**",
			"/", "/index.html", "/login", "/static/**", "/assets/**",
			"/*.js", "/*.css", "/*.ico", "/*.png", "/*.svg", "/*.json", "/error" };

	// Deliberately NOT the same list as PUBLIC_ENDPOINTS above: that list controls
	// *authentication*, this one controls *CSRF*, and they're different axes. Only
	// login/register are exempt here — there's no established session yet to protect,
	// and forced-login CSRF is a materially lower-severity issue than forcing a
	// state-changing action on an already-authenticated session. /auth/refresh and
	// /auth/logout are intentionally NOT exempt: both act on cookies, and by the time
	// either is called the frontend has already picked up an XSRF-TOKEN cookie from its
	// own GET /auth/me call on app load (GET requests aren't CSRF-checked, but the
	// CsrfCookieFilter below still issues the cookie on them).
	private static final String[] CSRF_IGNORED_ENDPOINTS = { "/auth/login", "/auth/register" };

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// The frontend reads the XSRF-TOKEN cookie's raw value directly (axios
		// withXSRFToken) and echoes it back verbatim as the X-XSRF-TOKEN header, so the
		// request handler must compare the raw value rather than the default
		// BREACH-protection XOR-masked one (that default is meant for tokens rendered
		// into server-side HTML forms, not read straight from a cookie by JS).
		CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
		requestHandler.setCsrfRequestAttributeName(null);

		http.csrf(csrf -> csrf.csrfTokenRepository(new PartitionedCookieCsrfTokenRepository())
						.csrfTokenRequestHandler(requestHandler).ignoringRequestMatchers(CSRF_IGNORED_ENDPOINTS))
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.requestMatchers(HttpMethod.GET, "/products", "/products/**", "/categories", "/categories/**", "/inventory/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/products/*/reviews", "/products/*/reviews/average").permitAll()
						.requestMatchers(HttpMethod.POST, "/products/*/reviews").authenticated()
						.requestMatchers(HttpMethod.POST, "/products/images", "/products/images/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/products", "/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/products", "/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/products", "/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/categories", "/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/categories", "/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/categories", "/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/inventory/**").hasRole("ADMIN")
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint()))
				.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
		return new AuthenticationEntryPoint() {
			@Override
			public void commence(HttpServletRequest request, HttpServletResponse response,
					org.springframework.security.core.AuthenticationException authException) throws IOException {
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				response.setCharacterEncoding("UTF-8");
				String body = "{\"success\":false,\"message\":\"Authentication required. Please provide a valid "
						+ "JWT token.\",\"timestamp\":\"" + java.time.Instant.now() + "\"}";
				response.getWriter().write(body);
			}
		};
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("https://x-cart.onrender.com", "http://localhost:5173"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Spring Security only writes the CSRF cookie lazily, when something actually reads
	 * the resolved CsrfToken value (normally a server-rendered HTML form tag). A pure
	 * JSON/SPA backend never does that, so without this filter the cookie would never
	 * appear at all. Forcing csrfToken.getToken() on every request is the standard
	 * Spring Security pattern for SPA CSRF integration — it's what makes the frontend's
	 * first GET /auth/me call (on every app load) actually deposit the XSRF-TOKEN
	 * cookie before the user does anything state-changing.
	 */
	private static final class CsrfCookieFilter extends OncePerRequestFilter {
		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {
			CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (csrfToken != null) {
				csrfToken.getToken();
			}
			filterChain.doFilter(request, response);
		}
	}
}