package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="oura_activity")
public class OuraActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String record_id;
    private int active_calories;
    private float average_met_minutes;
    private int meet_daily_targets;
    private int move_every_hour;
    private int recovery_time;
    private int stay_active;
    private int training_frequency;
    private int training_volume;

    private String day;
    private int equivalent_walking_distance;
    private int high_activity_met_minutes;
    private int high_activity_time;
    private int inactivity_alerts;
    private int low_activity_met_minutes;
    private int low_activity_time;
    private int medium_activity_met_minutes;
    private int medium_activity_time;

    private int meters_to_target;
    private int non_wear_time;
    private int resting_time;
    private int score;
    private int sedentary_met_minutes;
    private int sedentary_time;
    private int steps;
    private int target_calories;
    private int target_meters;

    private int total_calories;

    private OffsetDateTime timeRecord;

    private Long patient_id;
}
