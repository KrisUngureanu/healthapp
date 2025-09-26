package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="polar_spo")
public class PolarSpo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String source_device_id;
    private Long test_time;
    private int time_zone_offset;
    private String test_status;
    private int blood_oxygen_percent;
    private String spo2_class;
    private String spo2_value_deviation_from_baseline;
    private float spo2_quality_average_percent;
    private int average_heart_rate_bpm;
    private float heart_rate_variability_ms;
    private String spo2_hrv_deviation_from_baseline;
    private float altitude_meters;
    private OffsetDateTime start_time;
    private OffsetDateTime end_time;
}
