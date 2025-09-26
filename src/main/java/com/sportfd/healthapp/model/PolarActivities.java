package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name = "polar_activities")
public class PolarActivities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private OffsetDateTime start_time;
    private OffsetDateTime end_time;
    private String active_duration;
    private String inactive_duration;
    private float daily_activity;
    private int calories;
    private int active_calories;
    private int steps;
    private int inactivity_alert_count;
    private float distance_from_steps;

}
