package com.sportfd.healthapp.integration.polar;

import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PolarService {
    private final PolarActivitiesRepository polarActivitiesRepository;
    private final PolarCardioRepository polarCardioRepository;
    private final PolarExercisesRepository polarExercisesRepository;
    private final PolarHeartRateRepository polarHeartRateRepository;
    private final PolarNightRechargeRepository polarNightRechargeRepository;
    private final PolarSleepRepository polarSleepRepository;
    private final PolarSpoRepository polarSpoRepository;
    private final PolarTemperatureRepository polarTemperatureRepository;
    private final PolarTemperatureSampleRepository polarTemperatureSampleRepository;
    private final PolarTestECGRepository polarTestECGRepository;
    private final PolarUserInfoRepository polarUserInfoRepository;
    private final PolarHypnogramRepository polarHypnogramRepository;
    private final PolarHeartRateSamplesSleepRepository polarHeartRateSamplesSleepRepository;

    public List<PolarActivities> getPolarActivities(Long pid){
        return polarActivitiesRepository.findByPatientId(pid);
    }

    public List<PolarCardio> getPolarCardio(Long pid){
        return polarCardioRepository.findByPatientId(pid);
    }
    public List<PolarExercises> getPolarExercises(Long pid) {
        return polarExercisesRepository.findByPatientId(pid);
    }

    public List<PolarHeartRate> getPolarHeartRate(Long pid) {
        Pageable top10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "sampleTime"));
        return polarHeartRateRepository.findByPatientId(pid, top10);

    }

    public List<PolarNightRecharge> getPolarNightRecharge(Long pid) {
        return polarNightRechargeRepository.findByPatientId(pid);
    }

    public List<PolarSleep> getPolarSleep(Long pid) {
        return polarSleepRepository.findByPatientId(pid);
    }

    public List<PolarHypnogram> getPolarHypnogram(Long sleepid){
        return polarHypnogramRepository.findAllBySleepId(sleepid);
    }

    public List<PolarHeartRateSamplesSleep> getPolarHRSleep(Long sleepid){
        return polarHeartRateSamplesSleepRepository.findAllBySleepId(sleepid);
    }


    public List<PolarSpo> getPolarSpo(Long pid) {
        return polarSpoRepository.findByPatientId(pid);
    }

    public List<PolarTemperature> getPolarTemperature(Long pid) {
        Pageable top10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startTime"));
        return polarTemperatureRepository.findByPatientId(pid, top10);
    }

    public List<PolarTemperatureSample> getPolarTemperatureSamples(Long pid) {
        Pageable top10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "sampleTime"));
        return polarTemperatureSampleRepository.findByPatientId(pid, top10);
    }

    public List<PolarTestECG> getPolarTestEcg(Long pid) {
        return polarTestECGRepository.findByPatientId(pid);
    }


    public Optional<PolarUserInfo> getPolarUserInfo(Long pid) {
        return polarUserInfoRepository.findByPatientId(pid);
    }

}
