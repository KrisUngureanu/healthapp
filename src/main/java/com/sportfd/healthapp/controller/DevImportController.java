package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.model.SleepRecord;
import com.sportfd.healthapp.repo.SleepRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dev/import")
public class DevImportController {
    private final SleepRepository sleeps;
    public DevImportController(SleepRepository sleeps) { this.sleeps = sleeps; }

    @Operation(summary = "Импорт мок-сна для пациента")
    @PostMapping("/sleep")
    public List<SleepRecord> importSleep(@RequestParam Long patientId, @RequestBody List<SleepRecord> records) {
        for (SleepRecord r : records) r.setPatientId(patientId);
        return sleeps.saveAll(records);
    }
}