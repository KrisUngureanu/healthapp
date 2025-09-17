package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@Entity @Table(name="activity_daily")
public class ActivityDaily {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    @Enumerated(EnumType.STRING)
    private Provider provider;
    private LocalDate date;
    private Integer steps;
    private Integer calories;
    private Double distanceKm;
    private String sourceId;

    public ActivityDaily() {}

}