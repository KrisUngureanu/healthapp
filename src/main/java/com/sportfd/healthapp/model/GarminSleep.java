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
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "sleep_id", unique = true)
    private String sleepId;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "score")
    private Integer score;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    // Эти четыре колонки из DDL без кавычек => в БД они нижним регистром:
    @Column(name = "deepsleepduration")
    private Integer deepSleepDuration;

    @Column(name = "lightsleepduration")
    private Integer lightSleepDuration;

    @Column(name = "remsleep")
    private Integer remSleep;

    @Column(name = "awakeduration")
    private Integer awakeDuration;

    // Качественные оценки (snake_case в DDL)
    @Column(name = "total_duration_q", length = 16)
    private String totalDurationQ;

    @Column(name = "stress_q", length = 16)
    private String stressQ;

    @Column(name = "awake_count_q", length = 16)
    private String awakeCount;

    @Column(name = "rem_percentage_q", length = 16)
    private String remPercentage;

    @Column(name = "light_percentage_q", length = 16)
    private String lightPercentage;

    @Column(name = "deep_percentage_q", length = 16)
    private String deepPercentage;

    @Column(name = "restlessness_q", length = 16)
    private String restlessness;

}
