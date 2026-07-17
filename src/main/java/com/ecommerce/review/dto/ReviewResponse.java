package com.ecommerce.review.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

	private Long id;
	private Long productId;
	private String reviewerName;
	private int rating;
	private String comment;
	private Instant createdAt;
}
