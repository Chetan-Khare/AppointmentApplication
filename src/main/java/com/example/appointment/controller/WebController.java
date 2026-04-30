package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class WebController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/display")
    public String showPublicDisplay(Model model) {
        Appointment current = appointmentRepository.findByStatus("IN_PROGRESS").stream().findFirst().orElse(null);
        List<Appointment> upcoming = appointmentRepository.findByStatus("WAITING").stream().limit(5).collect(Collectors.toList());

        model.addAttribute("currentPatient", current);
        model.addAttribute("upcoming", upcoming);
        return "display";
    }

    @GetMapping("/api/slots")
    @ResponseBody
    public List<String> getAvailableSlots(@RequestParam("date") String dateStr, @RequestParam("doctorId") Long doctorId) {
        LocalDate selectedDate = LocalDate.parse(dateStr);
        User doctor = userRepository.findById(doctorId).orElse(null);
        if (doctor == null) return new ArrayList<>();

        List<String> bookedTimes = appointmentRepository.findByDateAndDoctorName(selectedDate, doctor.getFullName()).stream()
                .filter(a -> !"CANCELLED".equals(a.getStatus()))
                .map(Appointment::getTime)
                .collect(Collectors.toList());

        List<String> availableSlots = new ArrayList<>();
        LocalTime slotTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        while (slotTime.isBefore(endTime)) {
            String timeString = slotTime.toString();
            boolean isBooked = bookedTimes.contains(timeString);
            boolean isPast = selectedDate.equals(LocalDate.now()) && slotTime.isBefore(LocalTime.now());

            if (!isBooked && !isPast) {
                availableSlots.add(timeString);
            }
            slotTime = slotTime.plusMinutes(30);
        }
        return availableSlots;
    }
}