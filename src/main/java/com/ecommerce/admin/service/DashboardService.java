package com.ecommerce.admin.service;

import com.ecommerce.admin.dto.DashboardStatsResponse;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private final UserRepository userRepository;
	private final ProductRepository productRepository;
	private final OrderRepository orderRepository;
	private final InventoryRepository inventoryRepository;

	private static final int LOW_STOCK_THRESHOLD = 5;

	public DashboardStatsResponse getStats() {
		return DashboardStatsResponse.builder().totalUsers(userRepository.count())
				.totalProducts(productRepository.countByActiveTrue()).totalOrders(orderRepository.count())
				.totalRevenue(orderRepository.calculateTotalRevenue())
				.lowStockProductCount(inventoryRepository.countLowStock(LOW_STOCK_THRESHOLD))
				.pendingOrderCount(orderRepository.countByStatus(OrderStatus.PLACED)).build();
	}
}