package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
@Getter
@Setter
@Entity
@Table(name = "garmin_spo")
public class GarminSpo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String summaryId;
    private OffsetDateTime calendarDate;
    private Long patientId;

    @Column(columnDefinition = "text") private String timeOffsetSpoValues;
}
