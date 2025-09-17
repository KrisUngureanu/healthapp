package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@Entity @Table(name="recovery_records")
public class RecoveryRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    @Enumerated(EnumType.STRING)
    private Provider provider;
    private LocalDate date;
    private Integer rhr; // Resting HR
    private Integer hrv; // e.g. rMSSD
    private Integer recoveryScore; // whoop/oura readiness-like
    private String sourceId;

    public RecoveryRecord() {}

}

