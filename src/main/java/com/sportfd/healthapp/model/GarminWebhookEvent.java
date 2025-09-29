package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
@Getter
@Setter
@Entity
@Table(name = "garmin_webhook_events")
public class GarminWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id")
    Long patientId;

    @Column(name = "provider")
    @Enumerated(EnumType.STRING) private Provider provider = Provider.GARMIN;
    @Column(name = "event_type")
    private String eventType;     // e.g. "DAILY_SUMMARY", "SLEEP", "ACTIVITY", ...
    @Column(name = "user_id")
    private String userId;        // garmin user access reference
    @Column(name = "summary_id")
    private String summaryId;     // если присылают идентификаторы

    @Column(columnDefinition = "text", name = "payload_json") private String payloadJson;
    @Column(name = "received_at")
    private OffsetDateTime receivedAt;
}
