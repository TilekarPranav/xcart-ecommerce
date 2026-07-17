package com.ecommerce.review.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	Page<Review> findByProductId(Long productId, Pageable pageable);

	Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

	@Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
	Double findAverageRatingByProductId(@Param("productId") Long productId);

	@Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
	Long countByProductId(@Param("productId") Long productId);

}
