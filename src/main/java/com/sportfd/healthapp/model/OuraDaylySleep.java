package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="oura_daylysleep")
public class OuraDaylySleep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String record_id;
    private int deep_sleep;
    private int efficiency;
    private int latency;
    private int rem_sleep;
    private int restfulness;
    private int timing;
    private int total_sleep;
    private String day;
    private int score;
    private OffsetDateTime timeRecord;
    private Long patient_id;
}
