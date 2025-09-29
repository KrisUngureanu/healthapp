package com.sportfd.healthapp.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity @Table(name = "garmin_sleep")
public class GarminSleep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long patientId;
    private String sleepId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private int score;
    private int durationSec;
    @Column(columnDefinition = "text") private String payloadJson;
}
