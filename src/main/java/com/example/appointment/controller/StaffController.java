package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff") // All links start with /staff/...
public class StaffController {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private UserRepository userRepository;

    // 1. STAFF DASHBOARD
    @GetMapping("/dashboard")
    public String showStaffDashboard(Model model, Principal principal) {
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName());
            if (user != null) model.addAttribute("staffName", user.getFullName());
        }

        // Show Active Appointments (Waitlist)
        List<Appointment> all = appointmentRepository.findAll();
        List<Appointment> active = all.stream()
                .filter(a -> !"COMPLETED".equals(a.getStatus()) && !"CANCELLED".equals(a.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("appointments", active);
        return "staff_dashboard";
    }

    // 2. MARK PAID (Redirects back to Staff Dashboard)
    @GetMapping("/markPaid/{id}")
    public String markPaid(@PathVariable Long id) {
        Appointment appt = appointmentRepository.findById(id).orElse(null);
        if (appt != null) {
            appt.setPaymentStatus("PAID");
            appointmentRepository.save(appt);
        }
        return "redirect:/staff/dashboard";
    }

    // 3. CANCEL (Redirects back to Staff Dashboard)
    @GetMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {
        Appointment appt = appointmentRepository.findById(id).orElse(null);
        if (appt != null) {
            appt.setStatus("CANCELLED");
            appointmentRepository.save(appt);
        }
        return "redirect:/staff/dashboard";
    }
}