package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="oura_sleep")
public class OuraSleep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String record_id;
    private float average_breath;
    private float average_heart_rate;
    private int average_hrv;
    private int awake_time;
    private OffsetDateTime bedtime_end;
    private OffsetDateTime bedtime_start;
    private String day;
    private int deep_sleep_duration;
    private int efficiency;
    private int latency;
    private int light_sleep_duration;
    private int lowest_heart_rate;
    private int activity_balance;
    private int body_temperature;
    private int hrv_balance;
    private int previous_day_activity;
    private int previous_night;
    private int recovery_index;
    private int resting_heart_rate;
    private int sleep_balance;
    private int score;
    private float temperature_deviation;
    private int readiness_score_delta;
    private int rem_sleep_duration;
    private int restless_periods;
    private int time_in_bed;
    private int total_sleep_duration;
    private String type;
    private Long patient_id;

}
