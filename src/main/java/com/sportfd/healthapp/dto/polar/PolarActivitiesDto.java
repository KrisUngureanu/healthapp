package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record PolarActivitiesDto (
        @JsonProperty("start_time") String start_time,
        @JsonProperty("end_time") String end_time,
        @JsonProperty("active_duration") String active_duration,
        @JsonProperty("inactive_duration") String inactive_duration,
        @JsonProperty("daily_activity") float daily_activity,
        @JsonProperty("calories") int calories,
        @JsonProperty("active_calories") int active_calories,
        @JsonProperty("steps") int steps,
        @JsonProperty("inactivity_alert_count") int inactivity_alert_count,
        @JsonProperty("distance_from_steps") float distance_from_steps
)
{}
