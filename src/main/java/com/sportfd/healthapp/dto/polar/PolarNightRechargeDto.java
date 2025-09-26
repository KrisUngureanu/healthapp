package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record PolarNightRechargeDto(
        @JsonProperty("date") String date,
        @JsonProperty("heart_rate_avg") Integer heart_rate_avg,
        @JsonProperty("beat_to_beat_avg") Integer beat_to_beat_avg,
        @JsonProperty("heart_rate_variability_avg") Integer heart_rate_variability_avg,
        @JsonProperty("breathing_rate_avg") Float breathing_rate_avg
) {
}
