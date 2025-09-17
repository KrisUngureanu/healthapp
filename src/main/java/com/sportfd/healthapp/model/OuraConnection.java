package com.sportfd.healthapp.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Entity @Table(name = "oura_connections",
        uniqueConstraints = @UniqueConstraint(name="uk_oura_user", columnNames = "user_id"))
public class OuraConnection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;                   // ссылка на Users.id

    @Column(nullable=false, length=2048)
    private String accessToken;

    @Column(nullable=false, length=2048)
    private String refreshToken;

    @Column(nullable=false)
    private LocalDateTime expiresAt;      // когда access истечёт

    @Column(length=64)
    private String tokenType;             // "Bearer"

    @Column(length=256)
    private String scope;                 // какие scope пришли

    @Column(nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable=false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate void onUpdate(){ this.updatedAt = LocalDateTime.now(); }
}