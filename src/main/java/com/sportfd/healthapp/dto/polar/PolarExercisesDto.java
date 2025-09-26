package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;


public record PolarExercisesDto(

        @JsonProperty("record_id") String record_id,
        @JsonProperty("upload_time") String upload_time,
        @JsonProperty("device") String device,
        @JsonProperty("start_time") String start_time,
        @JsonProperty("start_time_utc_offset") int start_time_utc_offset,
        @JsonProperty("duration") String duration,
        @JsonProperty("distance") float distance,
        @JsonProperty("sport") String sport,
        @JsonProperty("has_route") boolean has_route,
        @JsonProperty("detailed_sport_info") String detailed_sport_info

) {
}
