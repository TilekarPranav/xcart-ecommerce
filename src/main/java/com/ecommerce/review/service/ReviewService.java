package com.ecommerce.review.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.dto.ReviewResponse;
import com.ecommerce.review.entity.Review;
import com.ecommerce.review.repository.ReviewRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	@Transactional
	public ReviewResponse create(Long productId, String email, ReviewRequest request) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

		reviewRepository.findByProductIdAndUserId(productId, user.getId()).ifPresent(r -> {
			throw new ConflictException("You have already reviewed this product");
		});

		Review review = Review.builder().product(product).user(user).rating(request.getRating())
				.comment(request.getComment()).build();

		Review saved = reviewRepository.save(review);
		return toResponse(saved);
	}

	@Transactional
	public ReviewResponse update(Long reviewId, String email, ReviewRequest request) {
		Review review = findByIdOrThrow(reviewId);

		if (!review.getUser().getEmail().equals(email)) {
			throw new ResourceNotFoundException("Review not found with id: " + reviewId);
		}

		review.setRating(request.getRating());
		review.setComment(request.getComment());

		Review saved = reviewRepository.save(review);
		return toResponse(saved);
	}

	@Transactional
	public void delete(Long reviewId, String email, boolean isAdmin) {
		Review review = findByIdOrThrow(reviewId);

		if (!isAdmin && !review.getUser().getEmail().equals(email)) {
			throw new ResourceNotFoundException("Review not found with id: " + reviewId);
		}

		reviewRepository.delete(review);
	}

	public Page<ReviewResponse> getByProductId(Long productId, Pageable pageable) {
		return reviewRepository.findByProductId(productId, pageable).map(this::toResponse);
	}

	public Double getAverageRating(Long productId) {
		Double avg = reviewRepository.findAverageRatingByProductId(productId);
		return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
	}

	private Review findByIdOrThrow(Long id) {
		return reviewRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
	}

	public ReviewResponse toResponse(Review review) {
		return ReviewResponse.builder().id(review.getId()).productId(review.getProduct().getId())
				.reviewerName(review.getUser().getName()).rating(review.getRating()).comment(review.getComment())
				.createdAt(review.getCreatedAt()).build();
	}

}
