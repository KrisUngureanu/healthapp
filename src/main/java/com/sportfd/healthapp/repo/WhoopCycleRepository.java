package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.WhoopCycle;
import com.sportfd.healthapp.model.WhoopRecovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WhoopCycleRepository extends JpaRepository<WhoopCycle, Long> {
    @Query("select w from WhoopCycle w where w.record_id = :recordId")
    Optional<WhoopCycle> findByRecordId(@Param("recordId") int recordId);

    @Query("select d from WhoopCycle d where d.patient_id = :pid")
    List<WhoopCycle> findByPatientId(@Param("pid") Long patientId);
}
