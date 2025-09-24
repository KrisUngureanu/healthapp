package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.model.Patient;
import com.sportfd.healthapp.model.Users;
import com.sportfd.healthapp.repo.PatientRepository;
import com.sportfd.healthapp.repo.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class UiController {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    public UiController(PatientRepository patientRepository, UserRepository userRepository) {
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
