package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;


@Setter
@Getter
@Entity
@Table(name="whoop_cycle")
public class WhoopCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int record_id;
    private int userid;
    private Long patient_id;

    private OffsetDateTime created_at;
    private OffsetDateTime updated_at;
    private OffsetDateTime start;
    private OffsetDateTime enddate;
    private String timezone_offset;
    private String score_state;
    private float strain; //нагрузка на сердце от 0 до 21
    private float kilojoule;
    private int average_heart_rate;
    private int max_heart_rate;
}
