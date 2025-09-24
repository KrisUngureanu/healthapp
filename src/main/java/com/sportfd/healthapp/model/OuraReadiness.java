package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Setter
@Getter
@Entity
@Table(name="oura_readiness")
public class OuraReadiness {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String record_id;
    private int activity_balance;
    private int body_temperature;
    private int hrv_balance;
    private int previous_day_activity;
    private int previous_night;
    private int recovery_index;
    private int resting_heart_rate;
    private int sleep_balance;
    private int sleep_regularity;
    private String day;
    private int score;
    private OffsetDateTime timeRecord;
    private Long patient_id;
}
