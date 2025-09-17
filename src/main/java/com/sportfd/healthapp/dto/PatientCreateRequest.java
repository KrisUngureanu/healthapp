package com.sportfd.healthapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientCreateRequest {
    private Long userId; // опционально (если хочешь линковать на существующего пользователя)

    @NotBlank
    @Size(min = 2, max = 150)
    private String fullname;

    // Active / Monitoring / Critical — свободная строка, можно позже сделать enum
    private String status;
}
