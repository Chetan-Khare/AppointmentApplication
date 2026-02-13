package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.model.User;
import com.example.appointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    @GetMapping("/dashboard")
    public String showDashboard(Model model, Principal principal) {
        String email = principal.getName();

        // 1. Fetch User Details (For "Welcome, Name")
        User user = userRepository.findByEmail(email);

        // 2. Fetch Appointments (For the table)
        List<Appointment> list = appointmentRepository.findByPatientEmail(email);

        // 3. Send data to HTML
        model.addAttribute("user", user);
        model.addAttribute("appointments", list);

        return "patient_dashboard"; // This must match your HTML filename
    }
}