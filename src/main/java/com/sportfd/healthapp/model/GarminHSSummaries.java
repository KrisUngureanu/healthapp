package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.GarminSummaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="garmin_hs_summaries")
public class GarminHSSummaries {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String summaryId;

    private GarminSummaryType summaryType;
    private float minValue;
    private float maxValue;
    private float avgValue;

}
