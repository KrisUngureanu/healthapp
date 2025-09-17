package com.sportfd.healthapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationRequest {
    private Long userId;

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;
    private String email;
    private String fullname;
    @NotBlank @Size(min = 6, max = 100)
    private String password;

    @NotBlank @Size(min = 6, max = 100)
    private String confirmPassword;
}
