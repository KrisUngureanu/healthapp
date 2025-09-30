package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="garmin_hrv")
public class GarminHRV {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String summaryId;
    private String calendarDate;
    private Integer lastNightAvg;
    @Column(name = "last_night_5min_high")
    private Integer lastNight5MinHigh;
    private Integer startTimeOffsetInSeconds;
    private Integer durationInSeconds;
    private Integer startTimeInSeconds;
    @Column(name = "hrv_values", columnDefinition = "text")
    private String hrvValues;
}
