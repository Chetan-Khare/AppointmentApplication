package com.example.appointment.controller;


import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.appointment.model.Appointment;
import com.example.appointment.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    // 1. This makes the HISTORY button work
    @GetMapping("/history")
    public String viewHistory(Model model) {
        // Matches the method name in your existing Service
        model.addAttribute("historyList", appointmentService.getCompletedAppointments());
        return "history";
    }

    // 2. This makes the COMPLETE button work
    @GetMapping("/complete/{id}")
    public String completeAppointment(@PathVariable Long id) {
        // Matches the method name in your existing Service
        appointmentService.completeAppointment(id);
        return "redirect:/queue";
    }
    @PostMapping("/book")
    public String book(@ModelAttribute Appointment appointment, Model model) {
        try {
            appointmentService.bookAppointment(appointment);
            return "redirect:/appointments"; // Success!
        } catch (Exception e) {
            // Send the error message back to the form
            model.addAttribute("errorMessage", e.getMessage());
            return "index";
        }
    }
    public void bookAppointment(Appointment appointment) {
        // 1. Get total appointments for today to decide the next token
        long count = appointmentRepository.count();
        appointment.setTokenNumber((int) count + 1); // Starts at 1, then 2, etc.

        // 2. Set default status if it's empty
        if (appointment.getStatus() == null) {
            appointment.setStatus("WAITING");
        }

        appointmentRepository.save(appointment);
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
    // Inside AppointmentController.java
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Clears the doctor's session
        return "redirect:/login"; // Sends them back to the login page
    }

    @GetMapping("/start/{id}")
    public String startAppointment(@PathVariable("id") Long id) {
        try {
            appointmentService.startAppointment(id);
        } catch (Exception e) {
            // Log error if any
            System.out.println("Error: " + e.getMessage());
        }
        // This redirects back to the table so the status updates
        return "redirect:/queue";
    }
}