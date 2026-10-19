package com.trustmarket.handshake.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    /** The Google ID token (JWT) returned by Google Identity Services in the browser. */
    @NotBlank
    private String idToken;
}
