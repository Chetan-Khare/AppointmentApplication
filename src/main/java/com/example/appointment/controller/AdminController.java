package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model, Principal principal) {
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName());
            if (user != null) model.addAttribute("adminName", user.getFullName());
        }

        // 1. Fetch System Stats
        List<Appointment> allAppointments = appointmentRepository.findAll();
        long totalDocs = userRepository.findAll().stream().filter(u -> "DOCTOR".equals(u.getRole())).count();
        long totalStaff = userRepository.findAll().stream().filter(u -> "STAFF".equals(u.getRole())).count();
        long totalPatients = userRepository.findAll().stream().filter(u -> "PATIENT".equals(u.getRole())).count();

        // 2. Add Stats to Model
        model.addAttribute("totalAppointments", allAppointments.size());
        model.addAttribute("totalDoctors", totalDocs);
        model.addAttribute("totalStaff", totalStaff);
        model.addAttribute("totalPatients", totalPatients);

        // 3. Show All Appointments (History & Active)
        model.addAttribute("appointments", allAppointments);

        // 4. Show All Users (Doctors & Staff)
        List<User> staffList = userRepository.findAll().stream()
                .filter(u -> "DOCTOR".equals(u.getRole()) || "STAFF".equals(u.getRole()))
                .toList();
        model.addAttribute("staffList", staffList);

        return "admin_dashboard";
    }
}