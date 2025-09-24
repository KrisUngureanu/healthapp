package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.WhoopRecovery;
import com.sportfd.healthapp.model.WhoopSleep;
import com.sportfd.healthapp.model.WhoopWorkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WhoopWorkoutRepository extends JpaRepository<WhoopWorkout, Long> {
    @Query("select w from WhoopWorkout w where w.record_id = :recordId")
    Optional<WhoopWorkout> findByRecordId(@Param("recordId") String recordId);

    @Query("select d from WhoopWorkout d where d.patient_id = :pid")
    List<WhoopWorkout> findByPatientId(@Param("pid") Long patientId);
}
