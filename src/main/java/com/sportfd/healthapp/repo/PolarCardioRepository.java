package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarCardio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PolarCardioRepository extends JpaRepository<PolarCardio, Long> {

    @Query("select w from PolarCardio w where w.date = :date and w.patientId = :patientId")
    Optional<PolarCardio> findByPatientIdAndDate(@Param("patientId") Long patientId, @Param("date") OffsetDateTime date);
    @Modifying
    @Transactional
    void deleteByPatientId(Long pid);

    List<PolarCardio> findByPatientId(Long pid);
}

