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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 1. Show Registration Form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new com.example.appointment.model.UserRegistrationDTO());
        return "register";
    }

    // 2. Process Registration
    @PostMapping("/register/save")
    public String registerUser(@ModelAttribute("user") com.example.appointment.model.UserRegistrationDTO registrationDto, RedirectAttributes ra) {
        // 1. Check if email already exists
        if (userRepository.findByEmail(registrationDto.getEmail()) != null) {
            ra.addFlashAttribute("error", "Email already registered!");
            return "redirect:/register";
        }

        // 2. Map DTO to Entity and set default role
        User user = new User();
        user.setFullName(registrationDto.getFullName());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole("PATIENT"); // Hardcoded here to prevent mass assignment

        userRepository.save(user);
        return "redirect:/login?success";
    }


    @GetMapping("/login")
    public String login() {
        return "login";
    }
}