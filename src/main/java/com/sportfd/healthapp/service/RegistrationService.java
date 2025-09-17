package com.sportfd.healthapp.service;

import com.sportfd.healthapp.dto.RegistrationRequest;
import com.sportfd.healthapp.model.Users;
import com.sportfd.healthapp.repo.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Users register(@Valid RegistrationRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (usersRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        Users u = new Users();
        // не меняем класс, используем как есть
        u.setUserId(req.getUserId()); // можно null
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword())); // храним ХЕШ
        u.setRole("DOCTOR");
        return usersRepository.save(u);
    }
}
