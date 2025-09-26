package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;


public record PolarTemperatureDto(
        @JsonProperty("source_device_id") String sourceDeviceId,
        @JsonProperty("start_time") String startTime,
        @JsonProperty("end_time") String endTime,
        @JsonProperty("measurement_type") String measurementType,
        @JsonProperty("sensor_location") String sensorLocation,
        @JsonProperty("samples") JsonNode samples
) {}