package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.ProviderClients;
import com.sportfd.healthapp.model.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class IntegrationsSyncController {

    private final ProviderClients clients;

    @PostMapping("/patients/{id}/integrations/{provider}/sync")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String sync(@PathVariable Long id,
                       @PathVariable String provider,
                       @RequestParam(defaultValue="sleep_daily") String type,
                       @RequestParam(defaultValue="7") int days) {
        var prov = Provider.valueOf(provider.toUpperCase());
        var client = clients.get(prov);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(1, days));

        int saved = switch (type) {
            case "sleep_daily"     -> client.syncSleepDaily(id, start, end);
            case "activity_daily"  -> client.syncActivityDaily(id, start, end);
            case "readiness_daily" -> client.syncReadinessDaily(id, start, end);
            case "sleep_sessions"  -> client.syncSleepSessions(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                    end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
            case "activity_sessions" -> client.syncActivitySessions(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                    end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
            case "hr_samples"       -> client.syncHeartRate(
                    id,end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                    end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
            case "spo2_samples"     -> client.syncSpO2(
                    id,end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                    end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
            default -> 0;
        };
        return "redirect:/patients/" + id + "?synced=" + saved + "&type=" + type + "&provider=" + provider;
    }


    @PostMapping("/patients/{id}/integrations/{provider}/syncAll")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncAll(@PathVariable Long id,
                       @PathVariable String provider,
                       @RequestParam(defaultValue="7") int days) {
        var prov = Provider.valueOf(provider.toUpperCase());
        var client = clients.get(prov);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(1, days));
        List<String> types = new ArrayList<>();
        types.add("sleep_daily");
        types.add("activity_daily");
        types.add("readiness_daily");
        types.add("sleep_sessions");
        types.add("activity_sessions");
        types.add("hr_samples");
        types.add("spo2_samples");
        for (String type: types){
            switch (type) {
                case "sleep_daily"     -> client.syncSleepDaily(id, start, end);
                case "activity_daily"  -> client.syncActivityDaily(id, start, end);
                case "readiness_daily" -> client.syncReadinessDaily(id, start, end);
                case "sleep_sessions"  -> client.syncSleepSessions(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                        end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
                case "activity_sessions" -> client.syncActivitySessions(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                        end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
                case "hr_samples"       -> client.syncHeartRate(
                        id,end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                        end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
                case "spo2_samples"     -> client.syncSpO2(
                        id,end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                        end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
            };
        }
        return "redirect:/patients/" + id;
    }


}