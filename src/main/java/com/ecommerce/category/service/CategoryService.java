package com.ecommerce.category.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ecommerce.category.dto.CategoryRequest;
import com.ecommerce.category.dto.CategoryResponse;
import com.ecommerce.category.entity.Category;
import com.ecommerce.category.repository.CategoryRepository;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (categoryRepository.existsByName(request.getName())) {
			throw new ConflictException("A category with this name already exists");
		}

		Category category = Category.builder().name(request.getName()).description(request.getDescription()).build();

		Category saved = categoryRepository.save(category);
		return toResponse(saved);
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = findByIdOrThrow(id);

		// If the name is changing, make sure the new name isn't already taken by
		// another category
		if (!category.getName().equalsIgnoreCase(request.getName())
				&& categoryRepository.existsByName(request.getName())) {
			throw new ConflictException("A category with this name already exists");
		}

		category.setName(request.getName());
		category.setDescription(request.getDescription());

		Category saved = categoryRepository.save(category);
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long id) {
		Category category = findByIdOrThrow(id);
		categoryRepository.delete(category);
	}

	public List<CategoryResponse> listAll() {
		return categoryRepository.findAll().stream().map(this::toResponse).toList();
	}

	private Category findByIdOrThrow(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
	}

	private CategoryResponse toResponse(Category category) {
		return CategoryResponse.builder().id(category.getId()).name(category.getName())
				.description(category.getDescription()).createdAt(category.getCreatedAt()).build();
	}
}
