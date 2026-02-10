package com.example.appointment.controller;

import com.example.appointment.model.User;
import com.example.appointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. Show Registration Form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // Matches your register.html file name
    }

    // 2. Process Registration
    @PostMapping("/register/save")
    public String registerUser(@ModelAttribute User user) {
        // Encrypt the password before saving!
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Default role if not provided
        if (user.getRole() == null) {
            user.setRole("PATIENT");
        }

        userRepository.save(user);
        return "redirect:/register?success";
    }

    // 3. Login Page (Optional, if you want a custom one)
    @GetMapping("/login")
    public String login() {
        return "login"; // You need to create login.html
    }
}