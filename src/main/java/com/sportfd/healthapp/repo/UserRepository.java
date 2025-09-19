package com.sportfd.healthapp.repo;

import com.sportfd.healthapp.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {
    boolean existsByUsername(String username);
    Optional<Users> findByUsername(String username);

    Users getDistinctById(Long id);

    Users getDistinctByUsername(String username);

    Users findByRole(String role);

    List<Users> findAllByRole(String role);

    // Берём роль напрямую из колонки role, не меняя entity
    @Query(value = "select role from users where username = :username", nativeQuery = true)
    String findRoleByUsername(String username);
}
