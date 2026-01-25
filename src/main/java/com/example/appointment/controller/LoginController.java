package com.example.appointment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {


    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        Model model) {

        // Simple hardcoded check for now
        if ("doctor1".equals(username) && "doctor123".equals(password)) {
            return "redirect:/queue";
        } else {
            model.addAttribute("error", "Invalid Credentials! Please try again.");
            return "login";
        }
    }
}
