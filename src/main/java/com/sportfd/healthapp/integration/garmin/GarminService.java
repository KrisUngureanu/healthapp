package com.sportfd.healthapp.integration.garmin;

import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GarminService {

    private final GarminActivityRepository garminActivityRepository;
    private final GarminSleepRepository garminSleepRepository;
    private final GarminDailySummaryRepository dailySummaryRepository;
    private final GarminSpoRepository spoRepository;
    private final GarminSpoValuesRepository spoValuesRepository;
    private final GarminTemperatureRepository temperatureRepository;
    private final GarminHealthSnapshotRepository healthSnapshotRepository;
    private final GarminHSSummariesRepository hsSummariesRepository;


    public List<GarminSleep> getGarminSleep(Long patientId){
        return garminSleepRepository.findAllByPatientId(patientId);
    }

    public List<GarminDailySummary> getDailySummary(Long patientId){
        return dailySummaryRepository.findAllByPatientId(patientId);
    }

    public List<GarminActivity> getActivity(Long patientId){
        return garminActivityRepository.findAllByPatientId(patientId);
    }

    public List<GarminSpo> getSpo(Long patientId){
       return spoRepository.findAllByPatientId(patientId);

    }

    public List<GarminSpoValues> getSpoValues(String summaryId){
        return spoValuesRepository.findAllBySummaryId(summaryId);
    }

    public List<GarminTemperature> getTemp(Long patientId){
        return temperatureRepository.findAllByPatientId(patientId);
    }

    public List<GarminHealthSnapshot> getHealthSnapshot(Long patientId){
        return healthSnapshotRepository.findAllByPatientId(patientId);
    }

    public List<GarminHSSummaries> getHssSumary(String summaryId){
        return hsSummariesRepository.findAllBySummaryId(summaryId);
    }

}
