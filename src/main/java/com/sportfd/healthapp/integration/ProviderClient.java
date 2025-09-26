package com.sportfd.healthapp.integration;

import com.sportfd.healthapp.model.enums.Provider;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

public interface ProviderClient {
    Provider provider();

    // OAuth
    String buildAuthorizeUrl(String state, String scopes, String redirectUri);
    void exchangeCodeAndSave(Long patientId, String code);
    void disconnect(Long patientId);


    default void syncCycles(Long pid, OffsetDateTime from, OffsetDateTime to) {};
    void syncSleep(Long pid, OffsetDateTime from, OffsetDateTime to);
    default void syncRecovery(Long pid, OffsetDateTime from, OffsetDateTime to){};
    default void syncWorkout(Long pid, OffsetDateTime from, OffsetDateTime to) {};
    void syncAll(Long pid, OffsetDateTime from, OffsetDateTime to);
    default void syncSleepSessions(Long patientId, OffsetDateTime from, OffsetDateTime to) {
    }
    default void syncActivityDaily(Long patientId, OffsetDateTime from, OffsetDateTime to) {
    }
    default void syncReadinessDaily(Long pid, OffsetDateTime from, OffsetDateTime to) {
    }

    default void syncCardio(Long pid, OffsetDateTime from, OffsetDateTime to) {}
    default void syncNightRecharge(Long pid, OffsetDateTime from, OffsetDateTime to) {}
    default void syncTemperature(Long pid, OffsetDateTime from, OffsetDateTime to) {}
    default void syncTestEcg(Long pid, OffsetDateTime from, OffsetDateTime to) {}
    default void syncUserInfo(Long pid, OffsetDateTime from, OffsetDateTime to) {}

    void syncHeartRate(Long patientId, OffsetDateTime from, OffsetDateTime to);


    void syncSpO2(Long patientId, OffsetDateTime from, OffsetDateTime to);

    default void deleteUser(Long pid, boolean alsoDisconnectLocally){}
}