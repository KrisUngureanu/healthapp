package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "garmin_spo_values")
public class GarminSpoValues {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String summaryId;
    @Column(name = "time_in_sec")
    private String timeinSec;
    private Integer value;
}
