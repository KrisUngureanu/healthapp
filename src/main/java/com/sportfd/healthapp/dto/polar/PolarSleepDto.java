package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

public record PolarSleepDto(
        @JsonProperty("polar_user") String polarUserUrl,
        @JsonProperty("date") LocalDate date,
        @JsonProperty("sleep_start_time") OffsetDateTime sleepStartTime,
        @JsonProperty("sleep_end_time") OffsetDateTime sleepEndTime,
        @JsonProperty("device_id") String deviceId,

        @JsonProperty("continuity") Double continuity,
        @JsonProperty("continuity_class") Integer continuityClass,

        @JsonProperty("light_sleep") Integer lightSleep,
        @JsonProperty("deep_sleep") Integer deepSleep,
        @JsonProperty("rem_sleep") Integer remSleep,
        @JsonProperty("unrecognized_sleep_stage") Integer unrecognizedSleepStage,

        @JsonProperty("sleep_score") Integer sleepScore,
        @JsonProperty("sleep_goal") Integer sleepGoal,
        @JsonProperty("sleep_rating") Integer sleepRating,

        @JsonProperty("total_interruption_duration") Integer totalInterruptionDuration,
        @JsonProperty("short_interruption_duration") Integer shortInterruptionDuration,
        @JsonProperty("long_interruption_duration") Integer longInterruptionDuration,

        @JsonProperty("sleep_cycles") Integer sleepCycles,

        @JsonProperty("group_duration_score") Integer groupDurationScore,
        @JsonProperty("group_solidity_score") Integer groupSolidityScore,
        @JsonProperty("group_regeneration_score") Double groupRegenerationScore,


        @JsonProperty("hypnogram") Map<String, Integer> hypnogram,
        @JsonProperty("heart_rate_samples") Map<String, Integer> heartRateSamples
) {}