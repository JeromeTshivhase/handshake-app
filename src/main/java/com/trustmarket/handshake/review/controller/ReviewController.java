package com.trustmarket.handshake.review.controller;

import com.trustmarket.handshake.review.dto.CreateReviewRequest;
import com.trustmarket.handshake.review.dto.ReviewResponse;
import com.trustmarket.handshake.review.service.ReviewService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import com.trustmarket.handshake.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create review", operationId = "createReview")
    public ApiResponse<ReviewResponse> create(Authentication auth, @Valid @RequestBody CreateReviewRequest req) {
        UUID reviewerId = AuthUtils.userId(auth);
        return ApiResponse.ok(ReviewResponse.from(reviewService.create(reviewerId, req)));
    }

    // Public: view reviews for a user (trust)
    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List reviews for user", operationId = "listReviewsForUser")
    public ApiResponse<Page<ReviewResponse>> listForUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.ok(reviewService.listForUser(userId, page, pageSize).map(ReviewResponse::from));
    }
}