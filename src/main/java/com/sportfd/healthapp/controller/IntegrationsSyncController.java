package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.ProviderClients;
import com.sportfd.healthapp.model.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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


    @PostMapping("/patients/{id}/integrations/polar/syncSleep")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarSleep(@PathVariable Long id,
                              @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        LocalDate end = LocalDate.now();
        client.syncSleep(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }



    @PostMapping("/patients/{id}/integrations/garmin/syncSleep")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncGarminSleep(@PathVariable Long id,
                                 @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.GARMIN);

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        client.syncSleep(id, from, to);

        return "redirect:/patients/" + id;
    }


    @PostMapping("/patients/{id}/integrations/polar/syncActivities")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarActivities(@PathVariable Long id,
                                 @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        OffsetDateTime end = OffsetDateTime.now();
        client.syncActivityDaily(id, end, end);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncCardio")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarCardio(@PathVariable Long id,
                                      @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        OffsetDateTime end = OffsetDateTime.now();
        client.syncCardio(id, end, end);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncExersises")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarExersises(@PathVariable Long id,
                                  @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        LocalDate end = LocalDate.now();
        client.syncWorkout(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncHR")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarHR(@PathVariable Long id,
                                     @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

       OffsetDateTime end = OffsetDateTime.now();
        client.syncHeartRate(id, end, end);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncAll")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarAll(@PathVariable Long id,
                              @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        OffsetDateTime end = OffsetDateTime.now();
        client.syncAll(id, end, end);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncNR")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarNR(@PathVariable Long id,
                              @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        OffsetDateTime end = OffsetDateTime.now();
        client.syncNightRecharge(id, end, end);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncSPO")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarSpo(@PathVariable Long id,
                              @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        LocalDate end = LocalDate.now();
        client.syncSpO2(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncTemp")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolartemp(@PathVariable Long id,
                               @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        OffsetDateTime end = OffsetDateTime.now();
        client.syncTemperature(id, end, end);

        return "redirect:/patients/" + id;
    }
    @PostMapping("/patients/{id}/integrations/polar/syncEcg")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarEcg(@PathVariable Long id,
                                @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        LocalDate end = LocalDate.now();
        client.syncTestEcg(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/polar/syncUserInfo")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncPolarUserInf(@PathVariable Long id,
                               @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.POLAR);

        LocalDate end = LocalDate.now();
        client.syncUserInfo(id, end.minusDays(Math.max(1,days)).atStartOfDay().atOffset(ZoneOffset.UTC),
                end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));

        return "redirect:/patients/" + id;
    }
    @DeleteMapping("/patients/{id}/integrations/{provider}/deleteUser")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String deleteUser(@PathVariable Long id, @PathVariable String provider,
                                   @RequestParam(defaultValue="7") int days){
        String providername = provider.toUpperCase();
        var client = clients.get(Provider.valueOf(providername));


        client.deleteUser(id, true);

        return "redirect:/";
    }
    @PostMapping("/patients/{id}/integrations/garmin/syncHR")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncGarminHR(@PathVariable Long id,
                                   @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.GARMIN);

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        client.syncHeartRate(id, from, to);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/garmin/syncSpO2")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncGarminSpO2(@PathVariable Long id,
                               @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.GARMIN);

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        client.syncSpO2(id, from, to);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/garmin/syncTemperature")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncGarminTemp(@PathVariable Long id,
                                 @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.GARMIN);

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        client.syncTemperature(id, from, to);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/garmin/syncAct")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncGarminAct(@PathVariable Long id,
                                 @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.GARMIN);

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        client.syncActivityDaily(id, from, to);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/garmin/syncHealthSnapshot")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncGarminHealthSnapshot(@PathVariable Long id,
                                @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.GARMIN);

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        client.syncHealthSnapshot(id, from, to);

        return "redirect:/patients/" + id;
    }

    @PostMapping("/patients/{id}/integrations/garmin/syncDaily")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public String syncGarminDaily(@PathVariable Long id,
                                @RequestParam(defaultValue="7") int days){
        var client = clients.get(Provider.GARMIN);

        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        client.syncDailySummary(id, from, to);

        return "redirect:/patients/" + id;
    }



}