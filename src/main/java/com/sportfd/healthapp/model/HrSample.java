package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity @Table(name="hr_samples")
public class HrSample {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    @Enumerated(EnumType.STRING)
    private Provider provider;
    private OffsetDateTime ts;
    private Integer bpm;
    private String sourceId;

    public HrSample() {}

}