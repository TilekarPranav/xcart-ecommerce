package com.ecommerce.order.service;

import com.ecommerce.config.KafkaTopicConfig;
import com.ecommerce.notification.event.OrderStatusChangedEvent;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderEventPublisher.class);
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final NotificationService notificationService;

	@Async
	public void publishOrderStatusEvent(Long userId, Long orderId, String newStatus) {
		String notificationMessage = "Your order #" + orderId + " is now " + newStatus;
		try {
			kafkaTemplate.send(KafkaTopicConfig.ORDER_STATUS_TOPIC, String.valueOf(orderId),
					new OrderStatusChangedEvent(userId, orderId, newStatus))
					.whenComplete((result, ex) -> {
						if (ex != null) {
							log.warn("Kafka send failed for order {}, creating notification via fallback: {}", orderId, ex.getMessage());
							try {
								notificationService.createNotification(userId, notificationMessage);
							} catch (Exception fallbackEx) {
								log.error("Fallback notification creation failed for user {} order {}", userId, orderId, fallbackEx);
							}
						}
					});
		} catch (Exception e) {
			log.warn("Failed to initiate Kafka publish for order {}. Creating notification via fallback: {}", orderId, e.getMessage());
			try {
				notificationService.createNotification(userId, notificationMessage);
			} catch (Exception fallbackEx) {
				log.error("Fallback notification creation failed for user {} order {}", userId, orderId, fallbackEx);
			}
		}
	}
}