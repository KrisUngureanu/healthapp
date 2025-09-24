package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="whoop_workout")
public class WhoopWorkout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String record_id;
    private int userid;
    private Long patient_id;
    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;
    private OffsetDateTime start;
    private OffsetDateTime enddate;

    private String timezone_offset;
    private String score_state;
    private String sport_name;
    private float strain; //нагрузка на сердце от 0 до 21
    private int average_heart_rate;
    private int max_heart_rate;
    private float kilojoule;
    private float percent_recorded;
    private float distance_meter;
    private float altitude_gain_meter;
    private float altitude_change_meter;

}
