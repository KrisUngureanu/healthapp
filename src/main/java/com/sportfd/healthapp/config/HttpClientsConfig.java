package com.sportfd.healthapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientsConfig {

    @Bean
    public WebClient ouraWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.ouraring.com")
                .build();
    }

    @Bean
    public WebClient ouraAuthClient() {
        return WebClient.builder()
                .baseUrl("https://cloud.ouraring.com")
                .build();
    }
}