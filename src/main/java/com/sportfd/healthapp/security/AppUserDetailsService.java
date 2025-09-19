package com.sportfd.healthapp.security;

import com.sportfd.healthapp.model.Users;
import com.sportfd.healthapp.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository usersRepository;
    @Override
    public UserDetails loadUserByUsername(String username) {
        var u = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        var role = usersRepository.findRoleByUsername(username);
        if (role == null || role.isBlank()) role = "ATHLETE";
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities("ROLE_" + role.toUpperCase())
                .build();
    }
}