package com.ecommerce.notification.service;

import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.repository.NotificationRepository;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;

	@Transactional
	public void createNotification(Long userId, String message) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Notification notification = Notification.builder().user(user).message(message).build();

		notificationRepository.save(notification);
	}

	public Page<NotificationResponse> getMyNotifications(String email, Pageable pageable) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return notificationRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
	}

	@Transactional
	public void markAsRead(Long notificationId, String email) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

		if (!notification.getUser().getEmail().equals(email)) {
			throw new ResourceNotFoundException("Notification not found");
		}

		notification.setRead(true);
		notificationRepository.save(notification);
	}

	private NotificationResponse toResponse(Notification notification) {
		return NotificationResponse.builder().id(notification.getId()).message(notification.getMessage())
				.read(notification.isRead()).createdAt(notification.getCreatedAt()).build();
	}
}