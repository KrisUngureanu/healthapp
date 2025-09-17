package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.ActivityDaily;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityDailyRepository extends JpaRepository<ActivityDaily, Long> {
}
