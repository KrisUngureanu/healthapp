package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="polar_nightrecharge")
public class PolarNightRecharge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private OffsetDateTime date;
    private int heart_rate_avg;
    private int beat_to_beat_avg;
    private int heart_rate_variability_avg;
    private float breathing_rate_avg;

}
