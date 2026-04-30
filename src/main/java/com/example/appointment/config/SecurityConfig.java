package com.example.appointment.config;

import com.example.appointment.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // 1. Inject your Custom Database Service
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**")) // Keep CSRF enabled for the rest
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // Better than disable()
                .authorizeHttpRequests(auth -> auth
                        // Public resources
                        .requestMatchers("/", "/register/**", "/login", "/display", "/book", "/api/slots", "/css/**", "/js/**", "/ws-appointment/**", "/error", "/payment/**", "/receipt/**", "/api/payment/**").permitAll()
                        
                        // Role-specific access
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/staff/**").hasRole("STAFF")
                        
                        // Shared/Authenticated
                        .requestMatchers("/download/prescription/**").authenticated()

                        // H2 Console
                        .requestMatchers("/h2-console/**").permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/default", true)
                        .permitAll()
                )
                .logout(logout -> logout.logoutSuccessUrl("/"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. This is the Bridge: It connects Spring Security to YOUR Database Service
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(customUserDetailsService); // Use DB, not RAM
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }
}