package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@Entity @Table(name="readiness_daily",
        uniqueConstraints = @UniqueConstraint(name="ux_readiness_daily",
                columnNames = {"patient_id","provider","day"}))
public class ReadinessDaily {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="patient_id", nullable=false) private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name="provider", nullable=false, length=32) private Provider provider;

    @Column(name="day", nullable=false) private LocalDate day;

    @Column(name="source_id", length=255) private String sourceId;

    private Integer score;                                    // readiness/recovery
    @Column(precision=5, scale=2) private BigDecimal strain;  // WHOOP (опционально)
    @Column(name="rhr_bpm") private Short rhrBpm;
    @Column(name="hrv_avg_ms") private Short hrvAvgMs;
    @Column(name="temp_deviation_c", precision=4, scale=2) private BigDecimal tempDeviationC;

    private String notes;
    @Type(JsonType.class)
    @Column(name="raw", columnDefinition="jsonb") private String raw;
}