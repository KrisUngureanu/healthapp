package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;


public record PolarSpoDto(
        @JsonProperty("source_device_id") String source_device_id,
        @JsonProperty("test_time") Long test_time,
        @JsonProperty("time_zone_offset") int time_zone_offset,
        @JsonProperty("test_status") String test_status,
        @JsonProperty("blood_oxygen_percent") int blood_oxygen_percent,
        @JsonProperty("spo2_class") String spo2_class,
        @JsonProperty("spo2_value_deviation_from_baseline") String spo2_value_deviation_from_baseline,
        @JsonProperty("spo2_quality_average_percent") float spo2_quality_average_percent,
        @JsonProperty("average_heart_rate_bpm") int average_heart_rate_bpm,
        @JsonProperty("heart_rate_variability_ms") float heart_rate_variability_ms,
        @JsonProperty("spo2_hrv_deviation_from_baseline") String spo2_hrv_deviation_from_baseline,
        @JsonProperty("altitude_meters") float altitude_meters,
        @JsonProperty("start_time") String start_time,
        @JsonProperty("end_time") String end_time
) {
}

