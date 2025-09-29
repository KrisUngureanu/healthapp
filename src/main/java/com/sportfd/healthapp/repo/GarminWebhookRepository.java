package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.GarminWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarminWebhookRepository extends JpaRepository<GarminWebhookEvent, Long> {
}
