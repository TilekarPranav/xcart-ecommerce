package com.ecommerce.notification.listener;

import com.ecommerce.notification.event.OrderStatusChangedEvent;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

	private final NotificationService notificationService;

	@Async
	@EventListener
	public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
		String message = "Your order #" + event.getOrderId() + " is now " + event.getNewStatus();
		notificationService.createNotification(event.getUserId(), message);
	}
}