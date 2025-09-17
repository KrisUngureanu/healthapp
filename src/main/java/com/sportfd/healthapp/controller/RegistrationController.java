package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.dto.RegistrationRequest;
import com.sportfd.healthapp.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
                                 BindingResult bindingResult,
                                 Model model) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "match", "Passwords do not match");
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            registrationService.register(request);
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            if (msg.contains("Username")) {
                bindingResult.rejectValue("username", "duplicate", msg);
            } else if (msg.contains("Passwords")) {
                bindingResult.rejectValue("confirmPassword", "match", msg);
            } else {
                bindingResult.reject("registrationError", msg);
            }
            return "register";
        }

        return "redirect:/login?registered";
    }
}
