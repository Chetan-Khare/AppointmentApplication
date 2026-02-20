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
        @Autowired
        private PasswordEncoder passwordEncoder; // <--- Add this line

        @Autowired
        private AppointmentRepository appointmentRepository;
        @Autowired
        private DoctorLeaveRepository leaveRepository;

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


        // --- ADD THESE 3 METHODS TO YOUR AppointmentController ---

        // 1. UPDATED ADMIN DASHBOARD (To handle Search)

        @GetMapping("/admin/dashboard")
        public String adminDashboard(Model model, Principal principal, @RequestParam(required = false) String doctorSearch) {
            if (principal != null) {
                User user = userRepository.findByEmail(principal.getName());
                if (user != null) model.addAttribute("adminName", user.getFullName());
            }

            model.addAttribute("totalAppointments", appointmentRepository.count());
            model.addAttribute("totalDoctors", userRepository.findByRole("DOCTOR").size());
            model.addAttribute("totalStaff", userRepository.findByRole("STAFF").size());
            model.addAttribute("totalPatients", appointmentRepository.findAll().stream().map(Appointment::getPatientEmail).distinct().count());

            // 1. Fetch Appointments with Search Logic
            List<Appointment> filteredAppointments; // Renamed to avoid conflicts
            if (doctorSearch != null && !doctorSearch.isEmpty()) {
                filteredAppointments = appointmentRepository.findAll().stream()
                        .filter(a -> a.getDoctorName() != null && a.getDoctorName().toLowerCase().contains(doctorSearch.toLowerCase()))
                        .collect(Collectors.toList());
                model.addAttribute("doctorSearch", doctorSearch);
            } else {
                filteredAppointments = appointmentRepository.findAll();
            }
            model.addAttribute("appointments", filteredAppointments);

            // 2. Fetch Staff List
            List<User> staffList = userRepository.findAll().stream()
                    .filter(u -> "DOCTOR".equals(u.getRole()) || "STAFF".equals(u.getRole()))
                    .collect(Collectors.toList());
            model.addAttribute("staffList", staffList);

            // 3. Chart Logic (Using unique variable names like 'statsDate' and 'statsMode')
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
            // Use your specific repository method for chart counts
            List<Object[]> statsDate = appointmentRepository.countAppointmentsByDate(sevenDaysAgo);

            Map<String, Long> dateMap = new LinkedHashMap<>();
            for (int i = 0; i < 7; i++) {
                dateMap.put(sevenDaysAgo.plusDays(i).toString(), 0L);
            }
            for (Object[] row : statsDate) {
                dateMap.put(row[0].toString(), (Long) row[1]);
            }
            model.addAttribute("chartDates", dateMap.keySet());
            model.addAttribute("chartCounts", dateMap.values());

            // 4. Doughnut Chart Logic
            List<Object[]> statsMode = appointmentRepository.countAppointmentsByPaymentMode();
            List<String> modes = new ArrayList<>();
            List<Long> modeCounts = new ArrayList<>();

            for (Object[] row : statsMode) {
                modes.add(row[0] != null ? row[0].toString() : "Unknown");
                modeCounts.add((Long) row[1]);
            }
            model.addAttribute("chartModes", modes);
            model.addAttribute("chartModeCounts", modeCounts);


            LocalDate today = LocalDate.now();

// 1. Calculate how many patients are WAITING today
            long waitingToday = appointmentRepository.findAll().stream()
                    .filter(a -> today.equals(a.getDate()))
                    .filter(a -> "WAITING".equals(a.getStatus()))
                    .count();

// 2. Calculate how many patients are COMPLETED today
            long completedToday = appointmentRepository.findAll().stream()
                    .filter(a -> today.equals(a.getDate()))
                    .filter(a -> "COMPLETED".equals(a.getStatus()))
                    .count();

// Send to HTML
            model.addAttribute("waitingToday", waitingToday);
            model.addAttribute("completedToday", completedToday);

            // 3. Calculate Today's Revenue (Bulletproof Version)
            String todayStr = LocalDate.now().toString(); // Safely converts to "YYYY-MM-DD"

            long paidTodayCount = appointmentRepository.findAll().stream()
                    // 1. Safe Date Check: Ensure date exists and matches today as a String
                    .filter(a -> a.getDate() != null && a.getDate().toString().equals(todayStr))
                    // 2. Safe Payment Check: Ensure status exists and matches "PAID"
                    .filter(a -> a.getPaymentStatus() != null && "PAID".equalsIgnoreCase(a.getPaymentStatus().trim()))
                    .count();

            long todaysRevenue = paidTodayCount * 500; // Multiply by consultation fee
            model.addAttribute("todaysRevenue", todaysRevenue);
// Send to HTML
            model.addAttribute("todaysRevenue", todaysRevenue);
            // 4. Calculate Doctors on Leave Today
            long doctorsOnLeaveToday = leaveRepository.findAll().stream()
                    .filter(l -> today.equals(l.getLeaveDate()))
                    .count();

// Send to HTML
            model.addAttribute("doctorsOnLeave", doctorsOnLeaveToday);


            return "admin_dashboard";
        }

        @GetMapping("/markPaid/{id}")
        public String markAsPaid(@PathVariable Long id, HttpServletRequest request) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if (appt != null) {
                appt.setPaymentStatus("PAID"); // This makes the revenue go up!
                appointmentRepository.save(appt);
            }
            // Stay on whatever dashboard the user is currently on
            String referer = request.getHeader("Referer");
            return "redirect:" + (referer != null ? referer : "/");
        }

        // 2. ASSIGN LEAVE (For both Staff and Doctors)
        @PostMapping("/admin/assign-leave")
        public String assignLeave(@RequestParam("userId") Long userId, @RequestParam("leaveDate") String leaveDate, RedirectAttributes ra) {
            try {
                com.example.appointment.model.DoctorLeave leave = new com.example.appointment.model.DoctorLeave();
                leave.setDoctorId(userId);
                leave.setLeaveDate(java.time.LocalDate.parse(leaveDate));
                leaveRepository.save(leave);
                ra.addFlashAttribute("message", "Leave assigned successfully!");
            } catch (Exception e) {
                ra.addFlashAttribute("error", "Failed: " + e.getMessage());
            }
            return "redirect:/admin/dashboard";
        }

        // 3. FULL CLINIC PDF REPORT
        @GetMapping("/admin/download/report")
        public ResponseEntity<InputStreamResource> downloadAdminReport() {
            List<Appointment> allAppointments = appointmentRepository.findAll();
            ByteArrayInputStream bis = pdfService.generateAdminReport(allAppointments);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=Clinic_Full_Report.pdf");
            return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
        }
        @GetMapping("/admin/cancel/{id}")
        public String adminCancel(@PathVariable Long id) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if(appt != null) {
                appt.setStatus("CANCELLED");
                appointmentRepository.save(appt);
            }
            return "redirect:/admin/dashboard";
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
        @GetMapping("/api/slots")
        @ResponseBody
        public List<String> getAvailableSlots(@RequestParam("date") String dateStr, @RequestParam("doctorId") Long doctorId) {
            LocalDate selectedDate = LocalDate.parse(dateStr);

            User doctor = userRepository.findById(doctorId).orElse(null);
            if (doctor == null) return new ArrayList<>();

            // Only fetch slots for THIS specific doctor
            List<String> bookedTimes = appointmentRepository.findAll().stream()
                    .filter(a -> a.getDate().equals(selectedDate))
                    .filter(a -> a.getDoctorName() != null && a.getDoctorName().equalsIgnoreCase(doctor.getFullName()))
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
        @GetMapping("/display")
        public String showPublicDisplay(Model model) {
            Appointment current = appointmentRepository.findAll().stream()
                    .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
                    .findFirst().orElse(null);

            List<Appointment> upcoming = appointmentRepository.findAll().stream()
                    .filter(a -> "WAITING".equals(a.getStatus()))
                    .limit(5)
                    .collect(Collectors.toList());

            model.addAttribute("currentPatient", current);
            model.addAttribute("upcoming", upcoming);
            return "display";
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
        public String callPatient(@RequestParam Long id, @RequestParam(defaultValue = "1") String cabin) {
            Appointment appt = appointmentRepository.findById(id).orElse(null);
            if (appt != null) {
                appt.setStatus("IN_PROGRESS");
                appointmentRepository.save(appt);

                // Bundle the appointment data WITH the cabin number into a Map
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("tokenNumber", appt.getTokenNumber());
                payload.put("patientName", appt.getPatientName());
                payload.put("cabin", cabin); // The new cabin data!

                // Send the custom payload to the TV display
                messagingTemplate.convertAndSend("/topic/appointment", payload);

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

        @GetMapping("/doctor/dashboard")
        public String doctorDashboard(Model model, Principal principal) {
            if (principal != null) {
                User doctor = userRepository.findByEmail(principal.getName());
                if (doctor != null) {
                    String searchName = doctor.getFullName().trim();
                    model.addAttribute("doctorName", searchName);

                    List<Appointment> myAppointments = appointmentRepository.findAll().stream()
                            .filter(a -> a.getDoctorName() != null && a.getDoctorName().trim().equalsIgnoreCase(searchName))
                            .filter(a -> "PAID".equalsIgnoreCase(a.getPaymentStatus()))
                            .filter(a -> "WAITING".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()))
                            .collect(Collectors.toList());

                    model.addAttribute("appointments", myAppointments);
                }
            }
            return "doctor_dashboard";
        }
        @GetMapping("/logout")
        public String logout() { return "redirect:/login"; }
    }