package com.ecommerce.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByUserId(Long userId, Pageable pageable);

	@Modifying
	@Query("DELETE FROM Notification n WHERE n.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);

}
