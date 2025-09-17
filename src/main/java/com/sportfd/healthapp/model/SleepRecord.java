package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity @Table(name="sleep_records")
public class SleepRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    @Enumerated(EnumType.STRING)
    private Provider provider;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Integer score;
    @Column(columnDefinition="jsonb")
    private String stagesJson;
    private String sourceId;

    public SleepRecord() {}

}