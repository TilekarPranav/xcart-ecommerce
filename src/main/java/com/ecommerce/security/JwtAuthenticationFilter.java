package com.ecommerce.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;

	@Override
	protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
			@NotNull FilterChain filterChain) throws ServletException, IOException {

		final String jwt = extractToken(request);

		if (jwt != null) {
			try {
				final String userEmail = jwtService.extractEmail(jwt);

				if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

					if (jwtService.isTokenValid(jwt, userDetails)) {
						UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails,
								null, userDetails.getAuthorities());
						token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(token);
					}
				}
			} catch (io.jsonwebtoken.ExpiredJwtException e) {
				log.debug("Expired JWT token for request to {}", request.getRequestURI());
			} catch (io.jsonwebtoken.JwtException e) {
				log.debug("Invalid JWT token for request to {}: {}", request.getRequestURI(), e.getMessage());
			}
		}

		filterChain.doFilter(request, response);
	}
	
	private String extractToken(HttpServletRequest request) {
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if ("accessToken".equals(c.getName())) return c.getValue();
			}
		}
		String header = request.getHeader("Authorization");
		return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
	}
}