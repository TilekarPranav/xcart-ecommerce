package com.ecommerce.order.service;

import com.ecommerce.config.KafkaTopicConfig;
import com.ecommerce.notification.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderEventPublisher.class);
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Async
	public void publishOrderStatusEvent(Long userId, Long orderId, String newStatus) {
		try {
			kafkaTemplate.send(KafkaTopicConfig.ORDER_STATUS_TOPIC, String.valueOf(orderId),
					new OrderStatusChangedEvent(userId, orderId, newStatus));
		} catch (Exception e) {
			log.warn("Failed to publish order status event to Kafka for order {}", orderId, e);
		}
	}
}