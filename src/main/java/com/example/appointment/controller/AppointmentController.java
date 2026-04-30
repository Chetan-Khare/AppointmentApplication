package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.DoctorLeaveRepository;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.service.AppointmentService;
import com.example.appointment.service.ReminderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.appointment.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import java.io.ByteArrayInputStream;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

    @Controller
    public class AppointmentController {
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AppointmentController.class);

        @Autowired
        private AppointmentRepository appointmentRepository;

        @Autowired
        private AppointmentService appointmentService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PdfService pdfService;

        @GetMapping("/")
        public String showHome(Model model) {
            model.addAttribute("appointment", new Appointment());
            model.addAttribute("doctorList", userRepository.findByRole("DOCTOR"));
            return "index";
        }

        @PostMapping("/book")
        public String bookAppointment(@ModelAttribute Appointment appointment, RedirectAttributes ra, Principal principal) {
            try {
                if (principal != null) {
                    appointment.setPatientEmail(principal.getName());
                }
                appointmentService.bookAppointment(appointment);
                
                if ("ONLINE".equalsIgnoreCase(appointment.getPaymentMode())) {
                    return "redirect:/payment/" + appointment.getPublicId();
                } else {
                    ra.addFlashAttribute("message", "Booking Confirmed!");
                    return "redirect:/receipt/" + appointment.getPublicId();
                }
            } catch (Exception e) {
                ra.addFlashAttribute("error", e.getMessage());
                return "redirect:/";
            }
        }

        @GetMapping("/payment/{publicId}")
        public String showPaymentPage(@PathVariable String publicId, Model model, Principal principal) {
            Appointment appt = appointmentRepository.findByPublicId(publicId);
            if (appt == null) return "redirect:/";

            // IDOR Protection: If logged in, must be owner. If guest, allow by UUID knowledge.
            if (principal != null && !appt.getPatientEmail().equals(principal.getName())) {
                return "redirect:/";
            }

            model.addAttribute("appointmentId", appt.getId());
            model.addAttribute("publicId", publicId);
            return "payment_gateway";
        }

    @GetMapping("/payment/success/{publicId}")
    public String paymentSuccess(@PathVariable String publicId, RedirectAttributes ra, Principal principal) {
        logger.info("Accessing payment success for publicId: {}", publicId);
        Appointment appt = appointmentRepository.findByPublicId(publicId);
        if (appt == null) return "redirect:/";

        // IDOR Protection: If logged in, must be owner. If guest, allow by UUID knowledge.
        if (principal != null && !appt.getPatientEmail().equals(principal.getName())) {
            return "redirect:/";
        }

        appointmentService.markAsPaid(appt.getId());
        ra.addFlashAttribute("message", "Payment Successful!");
        return "redirect:/receipt/" + publicId;
    }

    @GetMapping("/receipt/{publicId}")
    public String showReceipt(@PathVariable String publicId, Model model, Principal principal) {
        Appointment appt = appointmentRepository.findByPublicId(publicId);
        if (appt == null) return "redirect:/";

        // IDOR Protection
        if (principal != null && !appt.getPatientEmail().equals(principal.getName())) {
            return "redirect:/";
        }

        model.addAttribute("appt", appt);
        return "receipt";
    }

        @GetMapping("/download/prescription/{id}")
        public ResponseEntity<InputStreamResource> downloadPdf(@PathVariable Long id, Principal principal) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if (appt == null || principal == null || (!appt.getPatientEmail().equals(principal.getName()) && !isAdminOrDoctor(principal))) {
                return ResponseEntity.status(403).build(); // IDOR Protection
            }

            ByteArrayInputStream bis = pdfService.generatePrescriptionPdf(appt);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=prescription_" + appt.getTokenNumber() + ".pdf");
            return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
        }

        private boolean isAdminOrDoctor(Principal principal) {
            User user = userRepository.findByEmail(principal.getName());
            return user != null && (user.getRole().equals("ADMIN") || user.getRole().equals("DOCTOR"));
        }

        @GetMapping("/logout")
        public String logout(HttpServletRequest request) {
            return "redirect:/login?logout";
        }
    }