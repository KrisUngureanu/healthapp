package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarActivities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PolarActivitiesRepository extends JpaRepository<PolarActivities, Long> {
    @Query("select w from PolarActivities w where w.start_time = :start and w.patientId = :patientId")
    Optional<PolarActivities> findByPatientIdAndStart_time(@Param("patientId") Long patientId, @Param("start") OffsetDateTime start);
    @Modifying
    @Transactional
    void deleteByPatientId(Long pid);

    List<PolarActivities> findByPatientId(Long pid);
}
