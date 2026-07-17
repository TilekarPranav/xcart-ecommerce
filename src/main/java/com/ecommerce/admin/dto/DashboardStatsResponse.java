package com.ecommerce.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
	private long totalUsers;
	private long totalProducts;
	private long totalOrders;
	private BigDecimal totalRevenue;
	private long lowStockProductCount;
	private long pendingOrderCount;
}