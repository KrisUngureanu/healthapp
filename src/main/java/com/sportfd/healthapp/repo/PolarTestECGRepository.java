package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarTestECG;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PolarTestECGRepository extends JpaRepository<PolarTestECG, Long> {
    @Query("select w from PolarTestECG w where w.patientId = :patientId and w.test_time = :testTime")
    Optional<PolarTestECG> findByPatientIdAndTestTime(@Param("patientId") Long patientId, @Param("testTime") Long testTime);

    void deleteByPatientId(Long pid);
}
