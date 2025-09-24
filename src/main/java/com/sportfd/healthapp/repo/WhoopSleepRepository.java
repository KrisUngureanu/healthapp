package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.WhoopCycle;
import com.sportfd.healthapp.model.WhoopSleep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WhoopSleepRepository extends JpaRepository<WhoopSleep, Long> {
    @Query("select w from WhoopSleep w where w.record_id = :recordId")
    Optional<WhoopSleep> findByRecordId(@Param("recordId") String recordId);

    @Query("select d from WhoopSleep d where d.patient_id = :pid")
    List<WhoopSleep> findByPatientId(@Param("pid") Long patientId);
}
