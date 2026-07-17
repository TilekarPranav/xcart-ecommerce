package com.ecommerce.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

	@NotBlank(message = "Category name is required")
	@Size(max = 255, message = "Category name must be under 255 characters")
	private String name;

	@Size(max = 1000, message = "Description must be under 1000 characters")
	private String description;
}
