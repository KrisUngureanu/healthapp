package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter @Setter
@Entity @Table(name="sync_state",
        uniqueConstraints = @UniqueConstraint(name="ux_sync_state",
                columnNames = {"patient_id","provider"}))
public class SyncState {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="patient_id", nullable=false) private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(name="provider", nullable=false, length=32) private Provider provider;

    @Column(name="last_sleep_day") private LocalDate lastSleepDay;
    @Column(name="last_readiness_day") private LocalDate lastReadinessDay;
    @Column(name="last_activity_day") private LocalDate lastActivityDay;

    @Column(name="last_hr_ts") private OffsetDateTime lastHrTs;

    @Column(name="cursor", columnDefinition="jsonb") private String cursor;

    @Column(name="updated_at") private OffsetDateTime updatedAt;
}