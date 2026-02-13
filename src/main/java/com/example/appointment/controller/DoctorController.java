package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import java.util.List;

@Controller
public class DoctorController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/queue")
    public String showQueue(Model model, Principal principal) {
        // 1. Get Logged-in Doctor Name
        String email = principal.getName();
        User user = userRepository.findByEmail(email);
        model.addAttribute("doctorName", user.getFullName());

        // 2. Get List of Appointments
        List<Appointment> list = appointmentRepository.findAll();
        model.addAttribute("waitingList", list);

        return "queue";
    }
}