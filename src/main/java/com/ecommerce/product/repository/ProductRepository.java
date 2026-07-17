package com.ecommerce.product.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.product.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
	boolean existsByCategoryId(Long categoryId);

	@Query("""
			SELECT p FROM Product p
			WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
			  AND (:categoryId IS NULL OR p.category.id = :categoryId)
			  AND (:minPrice IS NULL OR p.price >= :minPrice)
			  AND (:maxPrice IS NULL OR p.price <= :maxPrice)
			  AND p.active = true
			""")
	Page<Product> search(@Param("name") String name, @Param("categoryId") Long categoryId,
			@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);

	long count(); // also free from JpaRepository

	@Query("SELECT COUNT(i) FROM Inventory i WHERE i.quantity < :threshold")
	long countLowStock(@Param("threshold") int threshold);
}
