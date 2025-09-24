package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.OuraActivity;
import com.sportfd.healthapp.model.OuraDaylySleep;
import com.sportfd.healthapp.model.OuraSleep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OuraDaylySleepRepository extends JpaRepository<OuraDaylySleep, Long> {
    @Query("select w from OuraDaylySleep w where w.record_id = :recordId")
    Optional<OuraDaylySleep> findByRecordId(@Param("recordId") String recordId);

    @Query("select d from OuraDaylySleep d where d.patient_id = :pid")
    List<OuraDaylySleep> findByPatientId(@Param("pid") Long patientId);
}
