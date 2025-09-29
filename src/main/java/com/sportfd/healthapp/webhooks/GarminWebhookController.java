package com.sportfd.healthapp.webhooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportfd.healthapp.integration.garmin.GarminClient;
import com.sportfd.healthapp.model.enums.Provider;
import com.sportfd.healthapp.model.GarminWebhookEvent;
import com.sportfd.healthapp.repo.ConnectionRepository;
import com.sportfd.healthapp.repo.GarminWebhookRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/garmin")
@RequiredArgsConstructor
public class GarminWebhookController {

    private final GarminClient garminClient;
    private final GarminWebhookRepository webhookRepo;
    private final ConnectionRepository connections;
    private final ObjectMapper om = new ObjectMapper();

    // Ping/health check — укажи этот URL в «Endpoint configuration» как ping
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("OK");
    }

    // Общая точка для Push/Notifications
    @PostMapping("/events")
    @Transactional
    public ResponseEntity<Void> onEvent(@RequestBody String body,
                                        @RequestHeader Map<String,String> headers,
                                        HttpServletRequest req) {
        // TODO: при необходимости — проверка IP/секретного пути/подписи из headers.
        try {
            JsonNode root = om.readTree(body);

            // Garmin может присылать массив событий; поддержим оба варианта
            if (root.isArray()) {
                for (JsonNode item : root) processOne(item);
            } else {
                processOne(root);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private void processOne(JsonNode evt) {
        // Примеры полей — подстрой под фактический payload твоего проекта
        String eventType = evt.path("eventType").asText(null);
        String userId    = evt.path("userId").asText(null);
        String summaryId = evt.path("summaryId").asText(null);

        GarminWebhookEvent rec = new GarminWebhookEvent();
        rec.setProvider(Provider.GARMIN);
        rec.setEventType(eventType);
        rec.setUserId(userId);
        rec.setSummaryId(summaryId);
        rec.setPayloadJson(evt.toString());
        rec.setReceivedAt(OffsetDateTime.now());
        webhookRepo.save(rec);

        // Пример логики «дотянуть детали» (если есть summaryId)
        // garminClient.pullDetailsByType(rec); // см. методы ниже
    }
}