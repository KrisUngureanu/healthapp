package com.sportfd.healthapp.integration.whoop;

import com.sportfd.healthapp.model.WhoopCycle;
import com.sportfd.healthapp.model.WhoopRecovery;
import com.sportfd.healthapp.model.WhoopSleep;
import com.sportfd.healthapp.model.WhoopWorkout;
import com.sportfd.healthapp.repo.WhoopCycleRepository;
import com.sportfd.healthapp.repo.WhoopRecoveryRepository;
import com.sportfd.healthapp.repo.WhoopSleepRepository;
import com.sportfd.healthapp.repo.WhoopWorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhoopService {

    private final WhoopRecoveryRepository whoopRecoveryRepository;
    private final WhoopSleepRepository whoopSleepRepository;
    private final WhoopWorkoutRepository whoopWorkoutRepository;
    private final WhoopCycleRepository whoopCycleRepository;

    public List<WhoopRecovery> getWhoopRecovery(Long pid){
        return whoopRecoveryRepository.findByPatientId(pid);
    }

    public List<WhoopSleep> getWhoopSleep(Long pid){
        return whoopSleepRepository.findByPatientId(pid);
    }

    public List<WhoopWorkout> getWhoopWorkout(Long pid){
        return whoopWorkoutRepository.findByPatientId(pid);
    }

    public List<WhoopCycle> getWhoopCycle(Long pid){
        return whoopCycleRepository.findByPatientId(pid);
    }
}
