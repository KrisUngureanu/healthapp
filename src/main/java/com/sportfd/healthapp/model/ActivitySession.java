package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;

@Getter @Setter
@Entity @Table(name="activity_sessions",
        uniqueConstraints = @UniqueConstraint(name="ux_activity_sessions",
                columnNames = {"patient_id","provider","source_id"}))
public class ActivitySession {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="patient_id", nullable=false) private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name="provider", nullable=false, length=32) private Provider provider;

    @Column(name="source_id", nullable=false, length=255) private String sourceId;

    @Column(name="start_time", nullable=false) private OffsetDateTime startTime;
    @Column(name="end_time",   nullable=false) private OffsetDateTime endTime;

    @Column(name="sport_type", length=64) private String sportType;
    private Integer calories;
    @Column(name="distance_m") private Integer distanceM;
    @Column(name="avg_hr") private Short avgHr;
    @Column(name="max_hr") private Short maxHr;
    private Integer load;
    @Type(JsonType.class)
    @Column(name="raw", columnDefinition="jsonb") private String raw;
}