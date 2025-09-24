package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.OuraHeartRate;
import com.sportfd.healthapp.model.OuraReadiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OuraReadinessRepository extends JpaRepository<OuraReadiness, Long> {
    @Query("select w from OuraReadiness w where w.record_id = :recordId")
    Optional<OuraReadiness> findByRecordId(@Param("recordId") String recordId);

    @Query("select d from OuraReadiness d where d.patient_id = :pid")
    List<OuraReadiness> findByPatientId(@Param("pid") Long patientId);



}
