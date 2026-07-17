package com.ecommerce.admin.controller;

import com.ecommerce.admin.dto.DashboardStatsResponse;
import com.ecommerce.admin.service.DashboardService;
import com.ecommerce.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

	private final DashboardService dashboardService;

	@GetMapping
	public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
		return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats()));
	}
}