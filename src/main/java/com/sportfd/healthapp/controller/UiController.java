package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.model.Patient;
import com.sportfd.healthapp.model.SleepRecord;
import com.sportfd.healthapp.model.Users;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.repo.SleepRepository;
import com.sportfd.healthapp.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
public class UiController {
    private final SleepRepository sleeps;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    public UiController(SleepRepository sleeps, PatientRepository patientRepository, UserRepository userRepository) { this.sleeps = sleeps;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails user, Model model) {

        String username = user.getUsername();
        Users userObj = userRepository.getDistinctByUsername(user.getUsername());
        String role = userObj.getRole();
        model.addAttribute("role", role);
        model.addAttribute("username", username);

        if (role.equals("ADMIN")){
            List<Users> doctors = userRepository.findAllByRole("DOCTOR");
            if (doctors != null){
                model.addAttribute("doctors", doctors);
            }

            return "homeadmin";
        } else {
            List<Patient> patients = patientRepository.findAllByDoctorId(userObj.getId());
            model.addAttribute("patients", patients);
            return "home";
        }

         }

    @GetMapping("/ui/patients/1/sleep")
    public String patientSleep(Model model) {
        var after = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        List<SleepRecord> data = sleeps.findByPatientIdAndStartTimeAfterOrderByStartTimeDesc(1L, after);
        model.addAttribute("items", data);

        // агрегаты
        int count = data.size();
        int avgScore = (int) data.stream().filter(s -> s.getScore() != null).mapToInt(SleepRecord::getScore).average().orElse(0);
        OffsetDateTime lastStart = data.isEmpty() ? null : data.get(0).getStartTime();
        model.addAttribute("count", count);
        model.addAttribute("avgScore", avgScore);
        model.addAttribute("lastStartTime", lastStart);

        // данные для графика (подготовим как строки и числа)
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM");
        var labels = data.stream().map(s -> s.getStartTime().format(df)).toList();
        var scores = data.stream().map(s -> s.getScore() == null ? 0 : s.getScore()).toList();
        model.addAttribute("labels", labels);
        model.addAttribute("scores", scores);

        return "patient-sleep";
    }

    @GetMapping("/privacy")
    public String getPrivacy(){
        return "privacy";
    }

    @GetMapping("/terms")
    public String getTerms(){
        return "terms";
    }



    @GetMapping("/login")
    public String getLogin(){
        return "login";
    }
    @GetMapping("/thanks")
    public String thanks(@RequestParam String provider,
                         @RequestParam Long pid,
                         Model model) {
        String providerName = switch (provider.toLowerCase()) {
            case "oura" -> "Oura";
            case "polar" -> "Polar";
            case "whoop" -> "WHOOP";
            case "garmin" -> "Garmin";
            default -> provider;
        };
        model.addAttribute("providerName", providerName);
        model.addAttribute("pid", pid);

        return "thanks";
    }

}
