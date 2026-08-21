package com.ecommerce.config;

import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@Profile("!test")
public class KafkaProducerConfig {

	@Bean
	public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> config = kafkaProperties.buildProducerProperties(null);
		// Fail fast instead of blocking the caller for up to 60s by default.
		config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
		config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
		return new DefaultKafkaProducerFactory<>(config);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}
}