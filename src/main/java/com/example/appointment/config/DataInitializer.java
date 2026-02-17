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
                doctor.setPassword(passwordEncoder.encode("password@123")); // Password
                doctor.setRole("DOCTOR"); // Role (Must be DOCTOR)

                userRepository.save(doctor);

                System.out.println("✅ DEFAULT DOCTOR CREATED: doctor@admin.com / password");
            }
            // 3. Create Admin (Super User)
            if (userRepository.findByEmail("admin@admin.com") == null) {
                User admin = new User();
                admin.setEmail("admin@admin.com");
                admin.setFullName("Big Boss");
                admin.setPassword(passwordEncoder.encode("admin123")); // Password
                admin.setRole("ADMIN"); // IMPORTANT: Role is ADMIN
                userRepository.save(admin);
                System.out.println("✅ DEFAULT ADMIN CREATED");
            }
            if (userRepository.findByEmail("staff@admin.com") == null) {
                User staff = new User();
                staff.setEmail("staff@admin.com");
                staff.setFullName("Receptionist Sarah");
                staff.setPassword(passwordEncoder.encode("password@123"));
                staff.setRole("STAFF"); // IMPORTANT: Role must be STAFF
                userRepository.save(staff);
            }
        };
    }
}