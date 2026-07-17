package com.ecommerce.category.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.category.entity.Category;
import java.util.List;


public interface CategoryRepository extends JpaRepository<Category, Long> {
	
	Optional<Category>  findByName(String name);
	boolean existsByName(String name);
}
