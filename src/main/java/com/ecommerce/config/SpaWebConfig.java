package com.ecommerce.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						Resource requestedResource = location.createRelative(resourcePath);
						if (requestedResource.exists() && requestedResource.isReadable()) {
							return requestedResource;
						}
						// Avoid forwarding API endpoints to index.html
						if (resourcePath.startsWith("auth") || 
							resourcePath.startsWith("api") || 
							resourcePath.startsWith("products") || 
							resourcePath.startsWith("categories") || 
							resourcePath.startsWith("cart") || 
							resourcePath.startsWith("orders") || 
							resourcePath.startsWith("payments") || 
							resourcePath.startsWith("inventory") || 
							resourcePath.startsWith("reviews") || 
							resourcePath.startsWith("users") || 
							resourcePath.startsWith("admin") || 
							resourcePath.startsWith("swagger") || 
							resourcePath.startsWith("v3")) {
							return null;
						}
						return new ClassPathResource("/static/index.html");
					}
				});
	}
}
