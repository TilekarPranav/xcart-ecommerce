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

		if (!isAllowedImage(file)) {
			throw new BadRequestException("Only JPEG, PNG, GIF, or WEBP image files are allowed");
		}

		if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank()) {
			throw new BadRequestException("Cloudinary credentials are not configured on the server");
		}

		try {
			Map uploadResult = cloudinary().uploader().upload(file.getBytes(),
					ObjectUtils.asMap("folder", "xcart-products"));
			String url = (String) uploadResult.get("secure_url");
			return ResponseEntity.ok(ApiResponse.success(url, "Image uploaded"));
		} catch (Exception e) {
			throw new BadRequestException("Failed to upload image: " + e.getMessage());
		}
	}

	private boolean isAllowedImage(MultipartFile file) {
		try {
			byte[] header = new byte[12];
			int read = file.getInputStream().read(header);
			if (read < 4)
				return false;

			// JPEG: FF D8 FF
			if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF)
				return true;
			// PNG: 89 50 4E 47
			if ((header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G')
				return true;
			// GIF: GIF87a / GIF89a
			if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F')
				return true;
			// WEBP: "RIFF"...."WEBP"
			if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
					&& header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P')
				return true;

			return false;
		} catch (IOException e) {
			return false;
		}
	}
}