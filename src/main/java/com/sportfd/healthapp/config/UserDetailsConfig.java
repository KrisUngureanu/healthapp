package com.sportfd.healthapp.config;

import com.sportfd.healthapp.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class UserDetailsConfig {

    private final UserRepository usersRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usersRepository.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPassword()) // тут уже должен быть BCrypt-хеш
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                        .accountExpired(false)
                        .accountLocked(false)
                        .credentialsExpired(false)
                        .disabled(false)
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}