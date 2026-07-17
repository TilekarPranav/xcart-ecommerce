package com.ecommerce.order.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Page<Order> findByUserId(Long userId, Pageable pageable);

	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'CANCELLED'")
	BigDecimal calculateTotalRevenue();

	long countByStatus(OrderStatus status);
}
