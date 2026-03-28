package com.trustmarket.handshake.user.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Size(max = 120)
    private String firstName;

    @Size(max = 120)
    private String lastName;

    @Size(max = 40)
    private String phone;

    @Size(max = 500)
    private String profilePictureUrl;
}