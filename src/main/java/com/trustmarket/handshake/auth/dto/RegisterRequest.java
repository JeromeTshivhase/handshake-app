package com.trustmarket.handshake.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @Email @NotBlank
    private String email;

    @NotBlank @Size(min = 8, max = 100)
    private String password;

    @NotBlank @Size(min = 8, max = 100)
    @JsonAlias({"confirmPassword"})
    private String passwordConfirm;

    @NotBlank @Size(max = 120)
    private String firstName;

    @NotBlank @Size(max = 120)
    private String lastName;

    @NotBlank @Size(max = 40)
    private String phone;

    @NotBlank @Size(max = 120)
    private String city;

    @NotBlank @Size(max = 120)
    private String province;
}