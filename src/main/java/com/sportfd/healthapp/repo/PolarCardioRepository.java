package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarCardio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PolarCardioRepository extends JpaRepository<PolarCardio, Long> {

    @Query("select w from PolarCardio w where w.date = :date and w.patientId = :patientId")
    Optional<PolarCardio> findByPatientIdAndDate(@Param("patientId") Long patientId, @Param("date") OffsetDateTime date);

    void deleteByPatientId(Long pid);
}

