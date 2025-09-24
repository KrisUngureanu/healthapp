package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.OuraReadiness;
import com.sportfd.healthapp.model.OuraSleep;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OuraSleepRepository extends JpaRepository<OuraSleep, Long> {
    @Query("select w from OuraSleep w where w.record_id = :recordId")
    Optional<OuraSleep> findByRecordId(@Param("recordId") String recordId);

    @Query("select d from OuraSleep d where d.patient_id = :pid")
    List<OuraSleep> findByPatientId(@Param("pid") Long patientId);
}
