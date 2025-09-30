package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "garmin_activity")
public class GarminActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long patientId;
    private String activityId;
    private String sport;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private int avgHr;
    private int maxHr;
    private int calories;
    private float distanceMeters;
    private String summaryId;

    private String activityName;
    private String activityDescription;
    private String activityType;
    private float averageHeartRateInBeatsPerMinute;
    private float averageRunCadenceInStepsPerMinute;
    private float averageSpeedInMetersPerSecond;
    private float averagePaceInMinutesPerKilometer;
    private Integer steps;



    @Column(columnDefinition = "text") private String payloadJson;
}
