package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarSpo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PolarSpoRepository extends JpaRepository<PolarSpo, Long> {
    @Query("select w from PolarSpo w where w.test_time = :start")
    Optional<PolarSpo> findByPatientIdAndTestTime(@Param("patientId") Long patientId, @Param("start") Long start);

    void deleteByPatientId(Long pid);
}
