package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Setter
@Entity @Table(name="spo2_samples")
public class Spo2Sample {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="patient_id", nullable=false) private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name="provider", nullable=false, length=32) private Provider provider;

    @Column(name="source_id", length=255) private String sourceId;

    @Column(name="ts", nullable=false) private OffsetDateTime ts;

    @Column(name="spo2_pct", nullable=false, precision=4, scale=1)
    private BigDecimal spo2Pct;
}