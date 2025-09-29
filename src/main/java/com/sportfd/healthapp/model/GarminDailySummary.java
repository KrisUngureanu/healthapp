package com.sportfd.healthapp.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity @Table(name = "garmin_daily_summary")
public class GarminDailySummary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long patientId;
    private String summaryId;
    private String day; // YYYY-MM-DD
    private int steps;
    private int calories;
    private int stress;
    private int bodyBattery;
    @Column(columnDefinition = "text") private String payloadJson;
    private OffsetDateTime updatedAt;
}
