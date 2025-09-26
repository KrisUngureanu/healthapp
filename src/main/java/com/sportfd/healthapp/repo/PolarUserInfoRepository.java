package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.PolarUserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PolarUserInfoRepository extends JpaRepository<PolarUserInfo, Long> {

    @Query("select w from PolarUserInfo w where w.patientId = :patientId")
    Optional<PolarUserInfo> findByPatientId(@Param("patientId") Long patientId);
    @Query("select w from PolarUserInfo w where w.polarUserId = :polarUserId")
    Optional<PolarUserInfo> findByPolarUserId(@Param("polarUserId") Long polarUserId);

    void deleteByPatientId(Long pid);
}
