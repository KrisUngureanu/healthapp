package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PolarHeartRateDto(@JsonProperty("polar_user") String polarUser,
                                @JsonProperty("date") String date,
                                @JsonProperty("heart_rate_samples") List<Sample> heartRateSamples
) {
    public record Sample(
            @JsonProperty("heart_rate") int heartRate,
            @JsonProperty("sample_time") String sampleTime // формат "HH:mm:ss"
    ) {
    }
}