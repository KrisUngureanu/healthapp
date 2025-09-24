package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name="oura_spo")
public class OuraSpo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String record_id;
    private int breathing_disturbance_index;
    private String day;
    private float spo2_percentage;
    private Long patient_id;

}
