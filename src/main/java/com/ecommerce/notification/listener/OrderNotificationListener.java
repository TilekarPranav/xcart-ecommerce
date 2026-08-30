package com.ecommerce.notification.listener;

import com.ecommerce.config.KafkaTopicConfig;
import com.ecommerce.notification.event.OrderStatusChangedEvent;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

	private final NotificationService notificationService;

	@KafkaListener(topics = KafkaTopicConfig.ORDER_STATUS_TOPIC, groupId = "xcart-notification-service")
	public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
		String message = "Your order #" + event.getOrderId() + " is now " + event.getNewStatus();
		notificationService.createNotification(event.getUserId(), message);
	}
}