package com.trustmarket.handshake.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshRequest {

    @NotBlank
    private String refreshToken;
}