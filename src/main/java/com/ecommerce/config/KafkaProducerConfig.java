package com.ecommerce.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@Profile("!test")
public class KafkaProducerConfig {

	@Value("${spring.kafka.bootstrap-servers:localhost:9092}")
	private String bootstrapServers;

	@Value("${spring.kafka.properties.security.protocol:PLAINTEXT}")
	private String securityProtocol;

	@Value("${spring.kafka.properties.ssl.truststore.location:}")
	private String truststoreLocation;

	@Value("${spring.kafka.properties.ssl.truststore.password:}")
	private String truststorePassword;

	@Value("${spring.kafka.properties.ssl.keystore.location:}")
	private String keystoreLocation;

	@Value("${spring.kafka.properties.ssl.keystore.password:}")
	private String keystorePassword;

	@Value("${spring.kafka.properties.ssl.key.password:}")
	private String keyPassword;

	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> config = new HashMap<>();
		config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		// Fail fast instead of blocking the caller for up to 60s by default.
		config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
		config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

		if ("SSL".equalsIgnoreCase(securityProtocol)) {
			config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
			if (truststoreLocation != null && !truststoreLocation.isBlank()) {
				config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
			}
			if (truststorePassword != null && !truststorePassword.isBlank()) {
				config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
			}
			if (keystoreLocation != null && !keystoreLocation.isBlank()) {
				config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
			}
			if (keystorePassword != null && !keystorePassword.isBlank()) {
				config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
			}
			if (keyPassword != null && !keyPassword.isBlank()) {
				config.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);
			}
		}

		return new DefaultKafkaProducerFactory<>(config);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}
}