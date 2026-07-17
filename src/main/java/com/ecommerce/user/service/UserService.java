package com.ecommerce.user.service;

import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.user.dto.AdminUserResponse;
import com.ecommerce.user.dto.ChangePasswordRequest;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserProfileResponse getProfile(String email) {
		User user = findByEmailOrThrow(email);
		return toProfileResponse(user);
	}

	@Transactional
	public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
		User user = findByEmailOrThrow(email);
		user.setName(request.getName());
		User saves = userRepository.save(user);
		return toProfileResponse(saves);
	}

	public void changePassword(String email, ChangePasswordRequest request) {
		User user = findByEmailOrThrow(email);

		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
			throw new BadRequestException("Current password is incorrect");
		}

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
	}

	public Page<AdminUserResponse> listUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(this::toAdminResponse);
	}

	@Transactional
	public void disableUser(Long userId) {
		User user = findByIdOrThrow(userId);
		user.setEnabled(false);
		userRepository.save(user);
	}

	@Transactional
	public void enableUser(Long userId) {
		User user = findByIdOrThrow(userId);
		user.setEnabled(true);
		userRepository.save(user);
	}

	@Transactional
	public void deleteUser(Long userId) {
		User user = findByIdOrThrow(userId);
		userRepository.delete(user);
	}

	private UserProfileResponse toProfileResponse(User user) {
		return UserProfileResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
				.roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())).build();
	}

	private AdminUserResponse toAdminResponse(User user) {
		return AdminUserResponse.builder().id(user.getId()).name(user.getName()).email(user.getEmail())
				.roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
				.enabled(user.isEnabled()).createdAt(user.getCreatedAt()).build();
	}

	private User findByEmailOrThrow(String email) {
		return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private User findByIdOrThrow(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id:" + id));
	}
}
