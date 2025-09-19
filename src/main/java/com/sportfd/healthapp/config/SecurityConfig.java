package com.sportfd.healthapp.config;

import com.sportfd.healthapp.security.AppUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AppUserDetailsService uds) throws Exception {
        http
                // Сообщаем Security, где брать юзеров
                .userDetailsService(uds)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/privacy", "/terms",
                                "/css/**", "/js/**", "/images/**",
                                "/oauth/**", "/thanks").permitAll()
                        .requestMatchers("/patients/*/integrations/*/invite").hasAnyRole("DOCTOR","ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/patients/*/integrations/*/disconnect").hasAnyRole("DOCTOR","ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/patients/*/integrations/*/sync").hasAnyRole("DOCTOR","ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        .key("healthapp-remember-me-key")
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 14)
                        .userDetailsService(uds)   // ← ВАЖНО! иначе будет "userDetailsService cannot be null"
                );

        return http.build();
    }
}

