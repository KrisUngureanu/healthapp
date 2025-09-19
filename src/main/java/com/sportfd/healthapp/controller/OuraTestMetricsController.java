package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.oura.OuraClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class OuraTestMetricsController {

    private final OuraClient oura;

    /** Табличка "шаги за N дней" — только для врача/админа */
    @GetMapping("/patients/{id}/integrations/oura/steps")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String showSteps(@PathVariable("id") Long patientId,
                            @RequestParam(name="days", defaultValue = "7") int days,
                            Model model) {

        var end = LocalDate.now();
        var start = end.minusDays(Math.max(1, days));

        var rows = oura.getDailySteps(patientId, start, end);

        model.addAttribute("patientId", patientId);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("rows", rows);
        return "oura-steps";
    }
}