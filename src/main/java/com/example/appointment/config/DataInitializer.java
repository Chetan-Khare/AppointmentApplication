package com.example.appointment.config;

import com.example.appointment.model.User;
import com.example.appointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // Check if the doctor already exists to avoid duplicates
            if (userRepository.findByEmail("doctor@admin.com") == null) {

                User doctor = new User();
                doctor.setEmail("doctor@admin.com"); // Login ID
                doctor.setFullName("Dr. House");
                doctor.setPassword(passwordEncoder.encode("password")); // Password
                doctor.setRole("DOCTOR"); // Role (Must be DOCTOR)

                userRepository.save(doctor);

                System.out.println("✅ DEFAULT DOCTOR CREATED: doctor@admin.com / password");
            }
        };
    }
}