package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="polar_testecg")
public class PolarTestECG {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source_device_id;
    private Long test_time;
    private Long patientId;
    private int time_zone_offset;
    private int average_heart_rate_bpm;
    private float heart_rate_variability_ms;
    private String heart_rate_variability_level;
    private float rri_ms;
    private float pulse_transit_time_systolic_ms;
    private float pulse_transit_time_diastolic_ms;
    private float pulse_transit_time_quality_index;
    private OffsetDateTime start_time;
    private OffsetDateTime end_time;

}
