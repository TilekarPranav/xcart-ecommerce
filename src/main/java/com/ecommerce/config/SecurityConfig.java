//package com.ecommerce.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.http.HttpMethod;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.AuthenticationProvider;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//import com.ecommerce.security.CustomUserDetailsService;
//import com.ecommerce.security.JwtAuthenticationFilter;
//
//import lombok.RequiredArgsConstructor;
//
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//	private final CustomUserDetailsService userDetailsService;
//	private final JwtAuthenticationFilter authenticationFilter;
//
//	private static final String[] PUBLIC_ENDPOINTS = { "/auth/register", "/auth/login", "/swagger-ui/**",
//			"/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**" };
//
//	@Bean
//	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//		http.csrf(csrf -> csrf.disable()).cors(cors -> {
//		}).authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
//				.requestMatchers(HttpMethod.GET, "/products/*/reviews", "/products/*/reviews/average").permitAll()
//				.requestMatchers(HttpMethod.POST, "/products/*/reviews").authenticated()
//				.requestMatchers(HttpMethod.POST, "/products/images/**").hasRole("ADMIN")
//				.requestMatchers(HttpMethod.GET, "/inventory/**").permitAll()
//				.requestMatchers(HttpMethod.PUT, "/inventory/**").hasRole("ADMIN")
//				.requestMatchers(HttpMethod.GET, "/products/**").permitAll()
//				.requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
//				.requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
//				.requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
//				.requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
//				.requestMatchers(HttpMethod.POST, "/categories/**").hasRole("ADMIN")
//				.requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
//				.requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN").requestMatchers("/admin/**")
//				.hasRole("ADMIN").anyRequest().authenticated())
//				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//				.authenticationProvider(authenticationProvider())
//				.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
//		return http.build();
//	}
//
//	@Bean
//	public CorsConfigurationSource corsConfigurationSource() {
//		CorsConfiguration config = new CorsConfiguration();
//		config.setAllowedOrigins(List.of("https://x-cart.onrender.com", "http://localhost:5173"));
//		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//		config.setAllowedHeaders(List.of("*"));
//		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//		source.registerCorsConfiguration("/**", config);
//		return source;
//	}
//
//	@Bean
//	public AuthenticationProvider authenticationProvider() {
//		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
//		provider.setPasswordEncoder(passwordEncoder());
//		return provider;
//	}
//
//	@Bean
//	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
//			throws Exception {
//		return authenticationConfiguration.getAuthenticationManager();
//	}
//
//	@Bean
//	public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
//	}
//}

package com.ecommerce.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.JwtAuthenticationFilter;

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
			"/auth/logout", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**" };

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("None").secure(true));
		http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
						.ignoringRequestMatchers("/auth/login", "/auth/register", "/auth/refresh", "/auth/logout"))
				.cors(cors -> {
				})
				.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.requestMatchers(HttpMethod.GET, "/products/*/reviews", "/products/*/reviews/average")
						.permitAll().requestMatchers(HttpMethod.POST, "/products/*/reviews").authenticated()
						.requestMatchers(HttpMethod.POST, "/products/images/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/inventory/**").permitAll()
						.requestMatchers(HttpMethod.PUT, "/inventory/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/products/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")
						.requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated())
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
				// Hand-written rather than serialized via ApiResponse/an ObjectMapper bean:
				// this response's shape is fixed, and different Spring Boot versions here
				// have swapped which Jackson major version's ObjectMapper/JsonMapper bean
				// actually exists — depending on either was the whole problem.
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
}