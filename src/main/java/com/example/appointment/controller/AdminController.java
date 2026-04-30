package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.model.User;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.repository.DoctorLeaveRepository;
import com.example.appointment.repository.UserRepository;
import com.example.appointment.service.AppointmentService;
import com.example.appointment.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorLeaveRepository leaveRepository;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Principal principal, @RequestParam(required = false) String doctorSearch) {
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName());
            if (user != null) model.addAttribute("adminName", user.getFullName());
        }

        // Stats from Service
        Map<String, Object> stats = appointmentService.getAdminDashboardStats();
        model.addAllAttributes(stats);

        // Chart Data from Service
        Map<String, Object> chartData = appointmentService.getChartData();
        model.addAttribute("chartDates", chartData.get("dates"));
        model.addAttribute("chartCounts", chartData.get("counts"));
        model.addAttribute("chartModes", chartData.get("modes"));
        model.addAttribute("chartModeCounts", chartData.get("modeCounts"));

        // Appointments with optimized fetching
        List<Appointment> filteredAppointments = appointmentService.getAppointmentsForAdmin(doctorSearch);
        model.addAttribute("appointments", filteredAppointments);
        model.addAttribute("doctorSearch", doctorSearch);

        // Staff List
        model.addAttribute("staffList", userRepository.findByRoleIn(List.of("DOCTOR", "STAFF")));

        // Leave stats
        model.addAttribute("doctorsOnLeave", leaveRepository.countByLeaveDate(LocalDate.now()));

        return "admin_dashboard";
    }

    @PostMapping("/assign-leave")
    public String assignLeave(@RequestParam("userId") Long userId, @RequestParam("leaveDate") String leaveDate, RedirectAttributes ra) {
        try {
            com.example.appointment.model.DoctorLeave leave = new com.example.appointment.model.DoctorLeave();
            leave.setDoctorId(userId);
            leave.setLeaveDate(LocalDate.parse(leaveDate));
            leaveRepository.save(leave);
            ra.addFlashAttribute("message", "Leave assigned successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/download/report")
    public ResponseEntity<InputStreamResource> downloadAdminReport() {
        List<Appointment> allAppointments = appointmentService.getAllAppointments();
        ByteArrayInputStream bis = pdfService.generateAdminReport(allAppointments);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=Clinic_Full_Report.pdf");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
    }

    @GetMapping("/cancel/{id}")
    public String adminCancel(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/confirm-pay/{id}")
    public String adminConfirmPay(@PathVariable Long id) {
        appointmentService.markAsPaid(id);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/add-user")
    public String addUser(@ModelAttribute User user, RedirectAttributes ra) {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            ra.addFlashAttribute("error", "Email already exists!");
            return "redirect:/admin/dashboard";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        ra.addFlashAttribute("message", "New " + user.getRole() + " created successfully!");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/history")
    public String viewAllHistory(Model model) {
        List<Appointment> history = appointmentService.getAllAppointments();
        model.addAttribute("appointments", history);
        return "appointments";
    }
}
