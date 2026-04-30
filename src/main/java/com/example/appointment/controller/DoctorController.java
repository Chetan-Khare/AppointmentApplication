package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.service.AppointmentService;
import com.example.appointment.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReminderService reminderService;

    @GetMapping("/dashboard")
    public String doctorDashboard(Model model, Principal principal) {
        if (principal != null) {
            User doctor = userRepository.findByEmail(principal.getName());
            if (doctor != null) {
                String searchName = doctor.getFullName();
                model.addAttribute("doctorName", searchName);
                
                List<Appointment> myAppointments = appointmentService.getDoctorAppointments(searchName);
                model.addAttribute("appointments", myAppointments);
            }
        }
        return "doctor_dashboard";
    }

    @PostMapping("/call")
    @ResponseBody
    public String callPatient(@RequestParam Long id, @RequestParam(defaultValue = "1") String cabin) {
        try {
            appointmentService.startAppointment(id, cabin);
            return "Called";
        } catch (Exception e) {
            return "Error";
        }
    }

    @PostMapping("/prescribe")
    public String savePrescription(@RequestParam Long id, @RequestParam String notes) {
        appointmentService.completeAppointment(id, notes);
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/markPaid/{id}")
    public String markAsPaid(@PathVariable Long id) {
        appointmentService.markAsPaid(id);
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/history")
    public String viewHistory(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "appointments";
    }
}
