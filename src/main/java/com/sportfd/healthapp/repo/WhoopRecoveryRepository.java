package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.OuraActivity;
import com.sportfd.healthapp.model.WhoopRecovery;
import com.sportfd.healthapp.model.WhoopSleep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WhoopRecoveryRepository extends JpaRepository<WhoopRecovery, Long> {
    @Query("select w from WhoopRecovery w where w.sleep_id = :recordId")
    Optional<WhoopRecovery> findByRecordId(@Param("recordId") String recordId);

    @Query("select d from WhoopRecovery d where d.patient_id = :pid")
    List<WhoopRecovery> findByPatientId(@Param("pid") Long patientId);
}
