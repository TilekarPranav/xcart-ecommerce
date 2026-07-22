package com.ecommerce.product.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.common.ApiResponse;
import com.ecommerce.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/products/images")
public class ProductImageController {

	@Value("${cloudinary.cloud-name}")
	private String cloudName;

	@Value("${cloudinary.api-key}")
	private String apiKey;

	@Value("${cloudinary.api-secret}")
	private String apiSecret;

	private Cloudinary cloudinary() {
		return new Cloudinary(ObjectUtils.asMap("cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<String>> upload(@RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			throw new BadRequestException("File is empty");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new BadRequestException("Only image files are allowed");
		}

		try {
			Map uploadResult = cloudinary().uploader().upload(file.getBytes(),
					ObjectUtils.asMap("folder", "xcart-products"));
			String url = (String) uploadResult.get("secure_url");
			return ResponseEntity.ok(ApiResponse.success(url, "Image uploaded"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to upload image", e);
		}
	}
}