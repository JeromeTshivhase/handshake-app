package com.trustmarket.handshake.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;

    @Size(max = 120, message = "City must be at most 120 characters")
    private String city;

    @Size(max = 120, message = "Province must be at most 120 characters")
    private String province;
}