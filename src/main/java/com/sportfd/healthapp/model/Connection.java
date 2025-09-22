package com.sportfd.healthapp.model;

import com.sportfd.healthapp.model.enums.Provider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity @Table(name="connections")
public class Connection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private Provider provider;
    @Column(columnDefinition="text")
    private String accessToken;
    @Column(columnDefinition="text")
    private String refreshToken;
    private OffsetDateTime expiresAt;
    @Column(columnDefinition="text")
    private String scope;
    private String externalUserId;

    public Connection() {}

    public void setTokenType(String bearer) {

    }
}