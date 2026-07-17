package com.ecommerce.product.controller;

import com.ecommerce.common.ApiResponse;
import com.ecommerce.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/products/images")
@RequiredArgsConstructor
public class ProductImageController {

	private final Path uploadDir = Paths.get("uploads/products");

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
			Files.createDirectories(uploadDir);
			String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
			String filename = UUID.randomUUID() + extension;
			Path target = uploadDir.resolve(filename);
			Files.copy(file.getInputStream(), target);

			String url = "/products/images/" + filename;
			return ResponseEntity.ok(ApiResponse.success(url, "Image uploaded"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to store file", e);
		}
	}

	@GetMapping("/{filename}")
	public ResponseEntity<Resource> serve(@PathVariable String filename) {
		try {
			Path file = uploadDir.resolve(filename);
			Resource resource = new UrlResource(file.toUri());
			if (!resource.exists()) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok().body(resource);
		} catch (MalformedURLException e) {
			return ResponseEntity.badRequest().build();
		}
	}
}