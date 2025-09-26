package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarExercises;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PolarExercisesRepository extends JpaRepository<PolarExercises, Long> {
    @Query("select w from PolarExercises w where w.start_time = :start and w.patientId = :patientId")
    Optional<PolarExercises> findByPatientIdAndStartTime(@Param("patientId") Long patientId, @Param("start") OffsetDateTime start);

    @Query("select w from PolarExercises w where w.record_id = :recordId")
    Optional<PolarExercises> findByRecordId(@Param("recordId") String recordId);
    @Modifying
    @Transactional
    void deleteByPatientId(Long pid);

    List<PolarExercises> findByPatientId(Long pid);
}
