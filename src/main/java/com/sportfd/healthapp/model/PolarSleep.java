package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "polar_sleep")
public class PolarSleep {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "sleep_start_time")
    private OffsetDateTime sleepStartTime;

    @Column(name = "sleep_end_time")
    private OffsetDateTime sleepEndTime;

    private String deviceId;

    private Double continuity;
    private Integer continuityClass;

    private Integer lightSleep;
    private Integer deepSleep;
    private Integer remSleep;
    private Integer unrecognizedSleepStage;

    private Integer sleepScore;
    private Integer sleepGoal;
    private Integer sleepRating;

    private Integer totalInterruptionDuration;
    private Integer shortInterruptionDuration;
    private Integer longInterruptionDuration;

    private Integer sleepCycles;

    private Integer groupDurationScore;
    private Integer groupSolidityScore;
    private Double groupRegenerationScore;

    @Column(columnDefinition = "text")
    private String hypnogramJson;

    @Column(columnDefinition = "text")
    private String heartRateSamplesJson;


}