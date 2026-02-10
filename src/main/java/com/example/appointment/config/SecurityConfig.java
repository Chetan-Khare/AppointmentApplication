package com.example.appointment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Add WebSocket paths to the permitAll list
                        .requestMatchers("/","/register/**", "/book", "/payment/**","/receipt/**","/display", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/ws-appointment/**").permitAll() // <--- ADD THIS LINE

                        // 2. Dashboard protection
                        .requestMatchers("/queue", "/appointments", "/start/**", "/complete/**","/cancel/**", "/prescription/**").hasRole("DOCTOR")
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/queue", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"));


        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // This hashes your passwords securely!
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Replace "doctor1" with a strong password. It will be hashed automatically.
        UserDetails doctor = User.builder()
                .username("doctor_admin")
                .password(passwordEncoder().encode("SecurePass2026!"))
                .roles("DOCTOR")
                .build();

        return new InMemoryUserDetailsManager(doctor);
    }
}