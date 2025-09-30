package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "garmin_daily_summary")
public class GarminDailySummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String summaryId;
    private String day;
    private Integer steps;
    private Integer activeKilocalories;
    private Integer bmrKilocalories;

    private Integer maxStressLevel;
    private Integer bodyBatteryChargedValue;
    private Integer bodyBatteryDrainedValue;
    @Column(columnDefinition = "text")
    private String payloadJson;
    private OffsetDateTime updatedAt;

    private String activityType;
    private Integer pushes;
    private Float distanceInMeters;
    private Float pushDistanceInMeters;
    private Integer floorsClimbed;
    private Integer minHeartRateInBeatsPerMinute;
    private Integer maxHeartRateInBeatsPerMinute;
    private Integer averageHeartRateInBeatsPerMinute;
    private Integer restingHeartRateInBeatsPerMinute;
    private Integer averageStressLevel;

}
