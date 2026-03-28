package com.trustmarket.handshake.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {

    @NotNull
    private UUID transactionId;

    @NotNull
    private UUID revieweeId;

    @Min(1) @Max(5)
    private int rating;

    @Size(max = 2000)
    private String comment;
}