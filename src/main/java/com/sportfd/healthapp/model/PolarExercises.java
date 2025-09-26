package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "polar_exercises")
public class PolarExercises {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String record_id;
    private OffsetDateTime upload_time;
    private String device;
    private OffsetDateTime start_time;
    private int start_time_utc_offset;
    private String duration;
    private float distance;
    private String sport;
    private boolean has_route;
    private String detailed_sport_info;
    private Integer average_heart_rate;
    private Integer max_heart_rate;
    private Integer calories;
    private Integer fat_percentage;
    private Integer carbohydrate_percentage;
    private Integer protein_percentage;
    private String device_id;

}
