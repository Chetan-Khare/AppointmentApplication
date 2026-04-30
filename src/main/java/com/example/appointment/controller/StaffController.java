package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String staffDashboard(Model model, Principal principal) {
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName());
            if (user != null) model.addAttribute("staffName", user.getFullName());
        }
        
        List<Appointment> appointments = appointmentService.getAllAppointments();
        model.addAttribute("appointments", appointments);
        
        return "staff_dashboard";
    }

    @GetMapping("/markPaid/{id}")
    public String markAsPaid(@PathVariable Long id) {
        appointmentService.markAsPaid(id);
        return "redirect:/staff/dashboard";
    }

    @GetMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return "redirect:/staff/dashboard";
    }
}