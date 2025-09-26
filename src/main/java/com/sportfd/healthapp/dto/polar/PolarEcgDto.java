package com.sportfd.healthapp.dto.polar;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record PolarEcgDto(
        @JsonProperty("source_device_id") String source_device_id,
        @JsonProperty("test_time") Long test_time,

        @JsonProperty("time_zone_offset") int time_zone_offset,
        @JsonProperty("average_heart_rate_bpm") int average_heart_rate_bpm,
        @JsonProperty("heart_rate_variability_ms") float heart_rate_variability_ms,
        @JsonProperty("heart_rate_variability_level") String heart_rate_variability_level,
        @JsonProperty("rri_ms") float rri_ms,
        @JsonProperty("pulse_transit_time_systolic_ms") float pulse_transit_time_systolic_ms,
        @JsonProperty("pulse_transit_time_diastolic_ms") float pulse_transit_time_diastolic_ms,
        @JsonProperty("pulse_transit_time_quality_index") float pulse_transit_time_quality_index,
        @JsonProperty("start_time") LocalDateTime start_time,
        @JsonProperty("end_time") LocalDateTime end_time
)
{}
