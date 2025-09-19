package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@Entity @Table(name="sleep_daily",
        uniqueConstraints = @UniqueConstraint(name="ux_sleep_daily",
                columnNames = {"patient_id","provider","day"}))
public class SleepDaily {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="patient_id", nullable=false) private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name="provider", nullable=false, length=32) private Provider provider;

    @Column(name="day", nullable=false) private LocalDate day;

    @Column(name="source_id", length=255) private String sourceId;

    @Column(name="total_sleep_sec") private Integer totalSleepSec;
    @Column(name="time_in_bed_sec") private Integer timeInBedSec;
    private Integer score;
    @Column(name="rhr_bpm") private Short rhrBpm;
    @Column(name="hrv_avg_ms") private Short hrvAvgMs;
    @Column(name="temp_deviation_c", precision=4, scale=2) private BigDecimal tempDeviationC;
    @Column(name="resp_rate", precision=4, scale=2) private BigDecimal respRate;
    @Type(JsonType.class)
    @Column(name="raw", columnDefinition="jsonb") private String raw;
}