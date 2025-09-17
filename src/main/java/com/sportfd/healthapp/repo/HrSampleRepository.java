package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.HrSample;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrSampleRepository extends JpaRepository<HrSample, Long> {
}
