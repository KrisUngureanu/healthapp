package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="garmin_health_snapshot")
public class GarminHealthSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String summaryId;
    private String calendarDate;
    private  Integer startTimeInSeconds;
    private Integer durationInSeconds;
    private Integer startTimeOffsetInSeconds;
    @Column(name = "summaries", columnDefinition = "text")
    private String summaries;
}
