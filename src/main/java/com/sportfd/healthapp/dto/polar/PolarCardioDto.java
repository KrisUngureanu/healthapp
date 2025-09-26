package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record PolarCardioDto(
        @JsonProperty("date") String date,
        @JsonProperty("cardio_load_status") String cardio_load_status,
        @JsonProperty("cardio_load_ratio") float cardio_load_ratio,
        @JsonProperty("cardio_load") float cardio_load,
        @JsonProperty("strain") float strain,
        @JsonProperty("tolerance") float tolerance
)
{}
