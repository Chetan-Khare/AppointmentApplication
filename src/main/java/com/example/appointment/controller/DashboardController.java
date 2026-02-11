package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.model.User;
import com.example.appointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    // 1. Smart Redirect: Decides where you go after login
    @GetMapping("/default")
    public String defaultAfterLogin(HttpServletRequest request) {
        if (request.isUserInRole("DOCTOR")) {
            return "redirect:/queue";
        }
        return "redirect:/dashboard";
    }


    // 2. The Patient Dashboard
    @GetMapping("/dashboard")
    public String showPatientDashboard(Model model, Principal principal) {
        String email = principal.getName(); // Get logged-in email
        User user = userRepository.findByEmail(email);

        // Find appointments for this specific email only
        List<Appointment> myAppointments = appointmentRepository.findByPatientEmail(email);

        model.addAttribute("user", user);
        model.addAttribute("appointments", myAppointments);
        return "patient_dashboard";
    }
}