package com.sportfd.healthapp.service;

import com.sportfd.healthapp.model.*;
import com.sportfd.healthapp.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientMetricsService {

    private final HrSampleRepository hrRepo;
    private final Spo2SampleRepository spo2Repo;
    private final ActivityDailyRepository actDailyRepo;
    private final ReadinessDailyRepository readyDailyRepo;
    private final SleepDailyRepository sleepDailyRepo;
    private final ActivitySessionRepository actSessRepo;
    private final SleepSessionRepository sleepSessRepo;

    public MetricsToday loadToday(Long patientId, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);

        // интервалы времени за сегодня в выбранной таймзоне
        OffsetDateTime from = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to   = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        // ряды
        List<HrSample> hr      = hrRepo.findByPatientIdAndTsBetweenOrderByTsAsc(patientId, from, to);
        List<Spo2Sample> spo2  = spo2Repo.findByPatientIdAndTsBetweenOrderByTsAsc(patientId, from, to);

        // daily
        List<ActivityDaily> activityDaily   = actDailyRepo.findByPatientIdAndDay(patientId, today);
        List<ReadinessDaily> readinessDaily = readyDailyRepo.findByPatientIdAndDay(patientId, today);
        List<SleepDaily> sleepDaily         = sleepDailyRepo.findByPatientIdAndDay(patientId, today);

        // sessions (опционально — закомментируй, если пока не нужно)
        List<ActivitySession> activitySessions = actSessRepo
                .findByPatientIdAndStartTimeBetweenOrderByStartTimeAsc(patientId, from, to);
        List<SleepSession> sleepSessions = sleepSessRepo
                .findByPatientIdAndStartTimeBetweenOrderByStartTimeAsc(patientId, from, to);

        return new MetricsToday(today, from, to, zone,
                hr, spo2, activityDaily, readinessDaily, sleepDaily, activitySessions, sleepSessions);
    }

    // Удобный форматер времени "HH:mm" в локальной зоне
    public static String fmtTime(OffsetDateTime ts, ZoneId zone) {
        return DateTimeFormatter.ofPattern("HH:mm")
                .format(ts.toInstant().atZone(zone));
    }

    // DTO-обёртка
    public record MetricsToday(
            LocalDate day,
            OffsetDateTime from, OffsetDateTime to, ZoneId zone,
            List<HrSample> hr,
            List<Spo2Sample> spo2,
            List<ActivityDaily> activityDaily,
            List<ReadinessDaily> readinessDaily,
            List<SleepDaily> sleepDaily,
            List<ActivitySession> activitySessions,
            List<SleepSession> sleepSessions
    ) {}
}