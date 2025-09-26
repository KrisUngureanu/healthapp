package com.sportfd.healthapp.dto.polar;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PolarUserInfoDto (
        @JsonProperty("polar-user-id") Long polarUserId,
        @JsonProperty("member-id") String memberId,
        @JsonProperty("registration-date") OffsetDateTime registrationDate,
        @JsonProperty("first-name") String firstName,
        @JsonProperty("last-name") String lastName,
        @JsonProperty("birthdate") LocalDate birthdate,
        @JsonProperty("gender") String gender,
        @JsonProperty("weight") Double weight,
        @JsonProperty("height") Double height
)
{}
