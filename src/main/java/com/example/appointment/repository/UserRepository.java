package com.example.appointment.repository;

import com.example.appointment.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    List<User> findByRole(String role);
    List<User> findByRoleIn(List<String> roles);
}