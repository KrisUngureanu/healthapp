package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="oura_heartrate")
public class OuraHeartRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private OffsetDateTime timeRecord;
    private int bpm;
    private String source;
    private Long patient_id;
}
