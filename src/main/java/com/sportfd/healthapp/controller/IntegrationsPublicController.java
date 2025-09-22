package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.integration.ProviderClients;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.util.InviteTokenUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class IntegrationsPublicController {

    private final InviteTokenUtil invites;
    private final ProviderClients clients;


    @Value("${app.oura.scopes:daily workout heartrate spo2 session}") private String ouraScopes;
    @Value("${app.whoop.scopes:offline read:profile read:sleep read:recovery read:workout read:cycles read:body_measurement}")
    private String whoopScopes;
    @GetMapping("/oauth/{provider}/start")
    public String start(@PathVariable String provider, @RequestParam("t") String token, HttpSession session, Model model) {
        Long pid = invites.verifyAndGetPatientId(token);
        if (pid == null) return "redirect:/privacy?error=invite";
        session.setAttribute("INTEG_PID", pid);
        session.setAttribute("INTEG_PROVIDER", provider.toUpperCase());
        model.addAttribute("patientId", pid);
        model.addAttribute("provider", provider.toUpperCase());
        String providerUpperCase = provider.toUpperCase();
        if (providerUpperCase.equals("OURA")){
            return "patient-oura-start";
        } else if (providerUpperCase.equals("WHOOP")) {
            return "patient-whoop-start";
        }
        return "patient-oura-start";
    }

    @GetMapping("/oauth/{provider}/connect")
    public String connect(@PathVariable String provider, HttpSession session) {
        Long pid = (Long) session.getAttribute("INTEG_PID");
        String p  = (String) session.getAttribute("INTEG_PROVIDER");
        if (pid == null || p == null || !p.equalsIgnoreCase(provider)) return "redirect:/privacy?error=no_ctx";
        String state = UUID.randomUUID().toString();
        session.setAttribute("INTEG_STATE", state);

        var prov = Provider.valueOf(p);
        String scopes = switch (prov) {
            case OURA -> ouraScopes;
            case WHOOP ->  whoopScopes;
            default -> "";
        };
        // redirectUri берётся из клиента (внутри)
        String url = clients.get(prov).buildAuthorizeUrl(state, scopes, null);
        return "redirect:" + url;
    }

    @GetMapping("/oauth/{provider}/callback")
    public String callback(@PathVariable String provider,
                           @RequestParam String code,
                           @RequestParam String state,
                           HttpSession session) {
        String expected = (String) session.getAttribute("INTEG_STATE");
        Long pid = (Long) session.getAttribute("INTEG_PID");
        String p = (String) session.getAttribute("INTEG_PROVIDER");
        session.removeAttribute("INTEG_STATE");
        if (expected == null || !expected.equals(state) || pid == null || p == null || !p.equalsIgnoreCase(provider)) {
            return "redirect:/privacy?error=state";
        }
        var prov = Provider.valueOf(p);
        clients.get(prov).exchangeCodeAndSave(pid, code);
        return "redirect:/thanks?provider=" + provider + "&pid=" + pid;
    }


}