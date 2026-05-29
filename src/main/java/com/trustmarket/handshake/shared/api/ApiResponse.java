package com.trustmarket.handshake.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    @Builder.Default
    private Instant timestamp = Instant.now();
    private boolean success;
    private T data;
    private ApiError error;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.timestamp = Instant.now();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        ApiResponse<T> r = new ApiResponse<>();
        r.timestamp = Instant.now();
        r.success = false;
        r.error = error;
        return r;
    }
}