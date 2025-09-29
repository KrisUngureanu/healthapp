package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="heart_rate_samples_sleep")
public class PolarHeartRateSamplesSleep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id")
    private Long patientId;
    @Column(name = "sleep_id")
    private Long sleepId;
    @Column(name = "sleep_time")
    private String sleepTime;
    @Column(name = "value_hr")
    private Long valueHr;
    @Column(name = "user_polar")
    private String userPolar;
}
