package com.ecommerce.review.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import com.ecommerce.review.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	Page<Review> findByProductId(Long productId, Pageable pageable);

	Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

	@Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
	Double findAverageRatingByProductId(@Param("productId") Long productId);

	@Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
	Long countByProductId(@Param("productId") Long productId);

	@Modifying
	@Query("DELETE FROM Review r WHERE r.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);

}
