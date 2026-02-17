package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.service.AppointmentService;
import com.example.appointment.service.ReminderService;
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
import java.util.List;
import java.util.stream.Collectors;

    @Controller
    public class AppointmentController {
        @Autowired
        private PasswordEncoder passwordEncoder; // <--- Add this line

        @Autowired
        private AppointmentRepository appointmentRepository;

        @Autowired
        private AppointmentService appointmentService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ReminderService reminderService;

        @Autowired
        private PdfService pdfService;

        @Autowired
        private SimpMessagingTemplate messagingTemplate;

        // --- 1. DOCTOR DASHBOARD (FIXED: Shows Active Only) ---
        @GetMapping("/doctor/dashboard")
        public String doctorDashboard(Model model, Principal principal) {
            // A. Get Doctor Name
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName());
                if (user != null) {
                    model.addAttribute("doctorName", user.getFullName());
                }
            }

            // B. Get ONLY Active Appointments (Hide Completed/Cancelled)
            List<Appointment> allAppointments = appointmentRepository.findAll();
            List<Appointment> paidActiveList = allAppointments.stream()
                    .filter(a -> "PAID".equals(a.getPaymentStatus()))
                    .filter(a -> !"COMPLETED".equals(a.getStatus()) && !"CANCELLED".equals(a.getStatus()))
                    .collect(Collectors.toList());

            model.addAttribute("appointments", paidActiveList);
            return "doctor_dashboard";
        }

        // --- 2. HISTORY PAGE (Shows Everything) ---
        @GetMapping("/appointments")
        public String viewAllHistory(Model model) {
            // Show ALL appointments (Completed, Cancelled, etc.)
            List<Appointment> history = appointmentRepository.findAll();
            model.addAttribute("appointments", history);
            return "appointments"; // Ensure you have appointments.html
        }
        @GetMapping("/download/prescription/{id}")
        public ResponseEntity<InputStreamResource> downloadPdf(@PathVariable Long id) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);

            if (appt != null) {
                ByteArrayInputStream bis = pdfService.generatePrescriptionPdf(appt);

                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Disposition", "attachment; filename=prescription_" + appt.getTokenNumber() + ".pdf");

                return ResponseEntity
                        .ok()
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(new InputStreamResource(bis));
            }
            return ResponseEntity.notFound().build();
        }

        // --- 3. SMART SLOTS API ---
        // --- 3. SMART SLOTS API ---
        @GetMapping("/api/slots")
        @ResponseBody
        public List<String> getAvailableSlots(@RequestParam("date") String dateStr) {
            // 1. Setup Dates
            LocalDate selectedDate = LocalDate.parse(dateStr);
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // 2. Get Booked Slots
            List<Appointment> booked = appointmentRepository.findByDate(selectedDate);
            List<String> bookedTimes = booked.stream()
                    .map(Appointment::getTime)
                    .collect(Collectors.toList());

            // 3. Generate Slots (9 AM to 5 PM)
            List<String> availableSlots = new ArrayList<>();
            LocalTime slotTime = LocalTime.of(9, 0); // Start at 09:00
            LocalTime endTime = LocalTime.of(17, 0); // End at 17:00

            while (slotTime.isBefore(endTime)) {
                String timeString = slotTime.toString(); // e.g., "09:00", "09:30"

                boolean isBooked = bookedTimes.contains(timeString);
                boolean isPast = false;

                // 4. "Time Travel" Check
                // If date is TODAY, check if this specific slot has passed
                if (selectedDate.equals(today)) {
                    if (slotTime.isBefore(now)) {
                        isPast = true;
                    }
                }

                // 5. Add if Valid
                if (!isBooked && !isPast) {
                    availableSlots.add(timeString);
                }

                // Increment by 30 mins (or 60 mins depending on your logic)
                slotTime = slotTime.plusMinutes(60);
            }

            return availableSlots;
        }

        // --- 4. PATIENT HOME ---
        @GetMapping("/")
        public String showHome(Model model) {
            model.addAttribute("appointment", new Appointment());
            // 1. Get list of doctors to show in dropdown
            List<User> doctors = userRepository.findByRole("DOCTOR");
            model.addAttribute("doctorList", doctors);
            List<Appointment> waitlist = appointmentRepository.findAll().stream()
                    .filter(a -> !"COMPLETED".equals(a.getStatus()) && !"PENDING_PAYMENT".equals(a.getStatus()))
                    .collect(Collectors.toList());
            model.addAttribute("waitlist", waitlist);
            return "index";
        }

        // --- 5. BOOKING LOGIC ---
        @PostMapping("/book")
        public String bookAppointment(@ModelAttribute Appointment appointment, RedirectAttributes ra) {
            try {
                long count = appointmentRepository.count();
                appointment.setTokenNumber((int) count + 1);

                if ("ONLINE".equalsIgnoreCase(appointment.getPaymentMode())) {
                    appointment.setStatus("PENDING_PAYMENT");
                    appointment.setPaymentStatus("PENDING");
                    appointmentRepository.save(appointment);
                    return "redirect:/payment/" + appointment.getId();
                } else {
                    appointment.setStatus("WAITING");
                    appointment.setPaymentStatus("PAY_AT_CLINIC");
                    appointmentRepository.save(appointment);
                    try { reminderService.sendBookingConfirmation(appointment); } catch (Exception e) {}

                    ra.addFlashAttribute("message", "Booking Confirmed!");
                    ra.addFlashAttribute("token", appointment.getTokenNumber());
                    return "redirect:/receipt/" + appointment.getId();
                }
            } catch (Exception e) { return "redirect:/"; }
        }

        // --- 6. DOCTOR ACTIONS ---
        @PostMapping("/doctor/call")
        @ResponseBody
        public String callPatient(@RequestParam Long id) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if (appt != null) {
                String message = "Token #" + appt.getTokenNumber() + " (" + appt.getPatientName() + ") - Room 1";
                messagingTemplate.convertAndSend("/topic/messages", "CALL:" + message);
                appt.setStatus("IN_PROGRESS");
                appointmentRepository.save(appt);
                return "Called";
            }
            return "Error";
        }

        @PostMapping("/doctor/prescribe")
        public String savePrescription(@RequestParam Long id, @RequestParam String notes) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if (appt != null) {
                appt.setPrescription(notes);
                appt.setStatus("COMPLETED"); // This removes it from Dashboard
                appointmentRepository.save(appt);
                try { reminderService.sendPrescription(appt, notes); } catch (Exception e) {}
            }
            return "redirect:/doctor/dashboard";
        }

        // --- 7. UTILITY PAGES ---
        @GetMapping("/payment/{id}")
        public String showPaymentPage(@PathVariable Long id, Model model) {
            model.addAttribute("appointmentId", id);
            return "payment_gateway";
        }

        @GetMapping("/payment/success/{id}")
        public String paymentSuccess(@PathVariable Long id, RedirectAttributes ra) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if (appt != null) {
                appt.setPaymentStatus("PAID");
                appt.setStatus("WAITING");
                appointmentRepository.save(appt);
                try { reminderService.sendBookingConfirmation(appt); } catch (Exception e) {}
                ra.addFlashAttribute("message", "Payment Successful!");
            }
            return "redirect:/receipt/" + id;
        }

        @GetMapping("/receipt/{id}")
        public String showReceipt(@PathVariable Long id, Model model) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            model.addAttribute("appt", appt);
            return "receipt";
        }

        @GetMapping("/display")
        public String showPublicDisplay(Model model) {
            Appointment current = appointmentRepository.findAll().stream()
                    .filter(a -> "IN_PROGRESS".equals(a.getStatus())).findFirst().orElse(null);
            model.addAttribute("currentPatient", current);
            return "display";
        }


        @GetMapping("/cancel/{id}")
        public String cancel(@PathVariable Long id) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if(appt != null) { appt.setStatus("CANCELLED"); appointmentRepository.save(appt); }
            return "redirect:/doctor/dashboard";
        }
        @GetMapping("/admin/confirm-pay/{id}")
        public String adminConfirmPay(@PathVariable Long id) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if (appt != null) {
                appt.setPaymentStatus("PAID");
                appointmentRepository.save(appt);
            }
            return "redirect:/admin/dashboard";
        }
        @PostMapping("/admin/add-user")
        public String addUser(@ModelAttribute User user, RedirectAttributes ra) {
            // 1. Check if email exists
            if (userRepository.findByEmail(user.getEmail()) != null) {
                ra.addFlashAttribute("error", "Email already exists!");
                return "redirect:/admin/dashboard";
            }

            // 2. ENCODE THE PASSWORD (Crucial Step!)
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);

            // 3. Save
            userRepository.save(user);

            ra.addFlashAttribute("message", "New " + user.getRole() + " created successfully!");
            return "redirect:/admin/dashboard";
        }
        @GetMapping("/logout")
        public String logout() { return "redirect:/login"; }
    }