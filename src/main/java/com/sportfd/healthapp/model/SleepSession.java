package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;

@Getter @Setter
@Entity @Table(name="sleep_sessions",
        uniqueConstraints = @UniqueConstraint(name="ux_sleep_sessions",
                columnNames = {"patient_id","provider","source_id"}))
public class SleepSession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="patient_id", nullable=false) private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name="provider", nullable=false, length=32) private Provider provider;

    @Column(name="source_id", nullable=false, length=255) private String sourceId;

    @Column(name="start_time", nullable=false) private OffsetDateTime startTime;
    @Column(name="end_time",   nullable=false) private OffsetDateTime endTime;

    @Column(name="duration_sec") private Integer durationSec;
    private Integer score;
    private Short efficiency;
    @Column(name="is_nap") private Boolean isNap;
    @Column(name="hr_avg") private Short hrAvg;
    @Column(name="hr_min") private Short hrMin;
    @Column(name="hrv_avg_ms") private Short hrvAvgMs;
    @Type(JsonType.class)
    @Column(name="raw", columnDefinition="jsonb") private String raw;
}