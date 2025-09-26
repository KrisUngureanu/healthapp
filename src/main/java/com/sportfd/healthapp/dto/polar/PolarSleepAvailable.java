package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PolarSleepAvailable(
        @JsonProperty("available") List<Item> available
) {
    public record Item(
            @JsonProperty("date") String date,
            @JsonProperty("start_time") String startTime,
            @JsonProperty("end_time") String endTime
    ) {}
}