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



    @PostMapping("/patients/{id}/integrations/whoop/syncCycle")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncCycle(@PathVariable Long id,
                          @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.WHOOP);

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(1, days));


        client.syncCycles(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/whoop/syncSleep")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncSleep(@PathVariable Long id,
                          @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.WHOOP);

        LocalDate end = LocalDate.now();



        client.syncSleep(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/whoop/syncRecovery")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncRecovery(@PathVariable Long id,
                            @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.WHOOP);

        LocalDate end = LocalDate.now();



        client.syncRecovery(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/whoop/syncWorkout")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncWorkout(@PathVariable Long id,
                               @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.WHOOP);

        LocalDate end = LocalDate.now();



        client.syncWorkout(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }
    @PostMapping("/patients/{id}/integrations/whoop/syncAll")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncAllWhoop(@PathVariable Long id,
                              @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.WHOOP);

        LocalDate end = LocalDate.now();



        client.syncAll(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/oura/syncSleep")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncOuraSleep(@PathVariable Long id,
                               @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.OURA);

        LocalDate end = LocalDate.now();



        client.syncSleep(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/oura/syncSleepSession")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncOuraSleepSession(@PathVariable Long id,
                                @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.OURA);

        LocalDate end = LocalDate.now();



        client.syncSleepSessions(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/oura/syncAll")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncOuraAll(@PathVariable Long id,
                                       @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.OURA);

        LocalDate end = LocalDate.now();



        client.syncAll(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }



}