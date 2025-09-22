package com.sportfd.healthapp.integration;

import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface ProviderClient {
    Provider provider();

    // OAuth
    String buildAuthorizeUrl(String state, String scopes, String redirectUri);
    void exchangeCodeAndSave(Long patientId, String code);
    void disconnect(Long patientId);


    default int syncSleepDaily(Long patientId, LocalDate start, LocalDate end) { return 0; }
    default int syncSleepSessions(Long patientId, OffsetDateTime from, OffsetDateTime to) { return 0; }
    default int syncActivityDaily(Long patientId, LocalDate start, LocalDate end) { return 0; }
    default int syncActivitySessions(Long patientId, OffsetDateTime from, OffsetDateTime to) { return 0; }
    default int syncReadinessDaily(Long patientId, LocalDate start, LocalDate end) { return 0; }





    @Transactional
    int syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to);

    @Transactional
    int syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to);
}