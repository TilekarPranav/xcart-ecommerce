package com.ecommerce.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderStatusChangedEvent {
	private Long userId;
	private Long orderId;
	private String newStatus;
}