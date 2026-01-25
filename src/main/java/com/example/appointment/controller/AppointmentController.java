package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import com.example.appointment.repository.AppointmentRepository;

import java.util.List;

@Controller
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository; // This matches the "symbol" Spring is looking for
    @GetMapping("/display")
    public String showPublicDisplay(Model model) {
        // Find the first patient currently being seen
        Appointment current = appointmentRepository.findAll().stream()
                .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
                .findFirst()
                .orElse(null);

        model.addAttribute("currentPatient", current);
        return "display"; // points to display.html
    }

    @Autowired
    private AppointmentService appointmentService;

    // 1. Shows all appointments (History)
    @GetMapping("/appointments")
    public String viewAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "appointments";
    }

    // 2. Doctor's Dashboard (Active Queue)
    @GetMapping("/queue")
    public String viewQueue(Model model) {
        // This only shows people who are WAITING or IN_PROGRESS
        model.addAttribute("waitingList", appointmentService.getWaitingQueue());
        return "queue";
    }

    // 3. Action to "Call" a patient
    @PostMapping("/queue/start/{id}")
    public String startAppointment(@PathVariable Long id) {
        appointmentService.startAppointment(id);
        return "redirect:/queue";
    }
}