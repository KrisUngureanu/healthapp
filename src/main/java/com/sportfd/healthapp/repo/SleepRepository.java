package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.SleepRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SleepRepository extends JpaRepository<SleepRecord, Long> {
    List<SleepRecord> findByPatientIdAndStartTimeAfterOrderByStartTimeDesc(Long patientId, OffsetDateTime after);
}
