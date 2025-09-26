package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(name="polar_cardio")
public class PolarCardio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private OffsetDateTime date;
    private String cardio_load_status;
    private float cardio_load_ratio;
    private float cardio_load;
    private float strain;
    private float tolerance;

}
