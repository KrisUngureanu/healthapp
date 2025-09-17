package com.sportfd.healthapp.controller;

import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.repo.ConnectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/connect")
public class ConnectController {
    private final ConnectionRepository connections;
    public ConnectController(ConnectionRepository connections) { this.connections = connections; }

    @Operation(summary = "Старт OAuth (заглушка)")
    @GetMapping("/{provider}/start")
    public ResponseEntity<String> start(@PathVariable Provider provider, @RequestParam Long patientId) {
        // TODO: собрать authorize URL провайдера и вернуть 302 redirect (сейчас — заглушка)
        return ResponseEntity.ok("Redirect user to OAuth of " + provider + " (patientId=" + patientId + ")");
    }

    @Operation(summary = "OAuth callback (заглушка обмена кода на токен)")
    @GetMapping("/{provider}/callback")
    public ResponseEntity<String> callback(@PathVariable Provider provider, @RequestParam String code, @RequestParam(required=false) Long state) {
        // TODO: обменять code->tokens, сохранить в connections
        return ResponseEntity.ok("Received code=" + code + " for provider=" + provider);
    }
}