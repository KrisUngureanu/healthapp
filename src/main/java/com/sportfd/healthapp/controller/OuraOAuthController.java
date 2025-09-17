package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.oura.OuraOAuthService;
import com.sportfd.healthapp.model.Users;
import com.sportfd.healthapp.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class OuraOAuthController {

    private final OuraOAuthService ouraService;
    private final UserRepository usersRepo;

    @GetMapping("/integrations")
    public String integrations(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("ouraConnected", /* вернёшь из репозитория */ false);
        return "integrations";
    }

    /** Кнопка "Connect Oura" — только для залогиненных */
    @GetMapping("/oauth/oura/connect")
    public String connect(@AuthenticationPrincipal UserDetails user) {
        String scopes = "daily workout heartrate spo2"; // оставь только нужные
        return "redirect:" + ouraService.buildAuthorizeUrl(scopes);
    }

    /** Callback из Oura (может быть доступен и анонимно, но лучше сессия та же) */
    @GetMapping("/oauth/oura/callback")
    public String callback(@RequestParam String code,
                           @AuthenticationPrincipal UserDetails user) {
        if (user == null) return "redirect:/login?from=oura";
        Users me = usersRepo.findByUsername(user.getUsername()).orElseThrow();
        ouraService.exchangeCodeAndSave(me.getId(), code);
        return "redirect:/integrations?oura=connected";
    }

    /** Disconnect */
    @PostMapping("/oauth/oura/disconnect")
    public String disconnect(@AuthenticationPrincipal UserDetails user) {
        Users me = usersRepo.findByUsername(user.getUsername()).orElseThrow();
        ouraService.disconnect(me.getId());
        return "redirect:/integrations?oura=disconnected";
    }

    /** Демка: показать сырые данные сна */
    @GetMapping("/oura/demo/sleep")
    @ResponseBody
    public String demoSleep(@AuthenticationPrincipal UserDetails user) {
        Users me = usersRepo.findByUsername(user.getUsername()).orElseThrow();
        return ouraService.getDailySleepJson(me.getId());
    }
}