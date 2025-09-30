package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
@Getter
@Setter
@Entity
@Table(name = "garmin_temperature")
public class GarminTemperature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String summaryId;
    private OffsetDateTime calendarDate;
    private Long patientId;
    private float avgDeviationCelsius;
    private Integer durationInSeconds;
    private Integer startTimeInSeconds;
    private Integer startTimeOffsetInSeconds;

}
