package com.trustmarket.handshake.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull
    private UUID handshakeId;

    /**
     * Must be an absolute http(s) URL; no javascript: or data: URIs.
     * Further domain restriction is enforced in the controller.
     */
    @NotBlank
    @Pattern(
            regexp = "^https?://[^\\s/$.?#].[^\\s]*$",
            message = "returnUrl must be a valid absolute HTTP/HTTPS URL"
    )
    private String returnUrl;
}