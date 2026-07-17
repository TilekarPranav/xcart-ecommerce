package com.ecommerce.auth.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserSummaryResponse;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.security.JwtService;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.RoleRepository;
import com.ecommerce.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ConflictException("An account with this email already exists");
		}

		Role customRole = roleRepository.findByName(Role.ROLE_CUSTOMER)
				.orElseThrow(() -> new IllegalStateException("ROLE_CUSTOMER not found - did DataInitializer run?"));

		Set<Role> roles = new HashSet<Role>();
		roles.add(customRole);

		User user = User.builder().name(request.getName()).email(request.getEmail())
				.passwordHash(passwordEncoder.encode(request.getPassword())).enabled(true).roles(roles).build();

		User saved = userRepository.save(user);
		UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getEmail());
		String accessToken = jwtService.generateAccessToken(userDetails);
		String refreshToken = jwtService.generateRefreshToken(userDetails);

		return buildAuthResponse(saved, accessToken, refreshToken);
	}

	public AuthResponse login(LoginRequest request) {
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new IllegalStateException("User authenticated but not found in DB"));

		UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
		String accessToken = jwtService.generateAccessToken(userDetails);
		String refreshToken = jwtService.generateRefreshToken(userDetails);

		return buildAuthResponse(user, accessToken, refreshToken);
	}

	public UserSummaryResponse me(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));

		return UserSummaryResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
				.roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())).build();
	}

	private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
		// TODO Auto-generated method stub
		return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
				.userId(user.getId()).name(user.getName()).email(user.getEmail()).build();
	}
}
