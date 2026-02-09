package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;

    // --- 1. PATIENT SIDE: Booking Form & Live Waitlist ---
    @GetMapping("/")
    public String showHome(Model model) {
        model.addAttribute("appointment", new Appointment());

        // FIX: Filter out "PENDING_PAYMENT" appointments so they don't show in the public queue
        List<Appointment> waitlist = appointmentRepository.findAll().stream()
                .filter(a -> !"COMPLETED".equals(a.getStatus())
                        && !"PENDING_PAYMENT".equals(a.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("waitlist", waitlist);

        return "index"; // Matches your index.html
    }

    @PostMapping("/book")
    public String bookAppointment(@ModelAttribute Appointment appointment, RedirectAttributes ra) {
        try {
            System.out.println("DEBUG: Payment Mode Received = " + appointment.getPaymentMode());

            // 1. Calculate Token Number
            long count = appointmentRepository.count();
            appointment.setTokenNumber((int) count + 1);

            // 2. Check Payment Mode
            String mode = appointment.getPaymentMode();

            if (mode != null && mode.equalsIgnoreCase("ONLINE")) {
                // --- ONLINE FLOW ---
                // Save as PENDING so they don't appear in the queue yet
                appointment.setStatus("PENDING_PAYMENT");
                appointment.setPaymentStatus("PENDING");
                appointmentRepository.save(appointment);

                // Redirect to payment page
                return "redirect:/payment/" + appointment.getId();

            } else {
                // --- CASH FLOW ---
                // Go straight to the queue
                appointment.setStatus("WAITING");
                appointment.setPaymentStatus("PAY_AT_CLINIC");
                appointmentRepository.save(appointment);

                ra.addFlashAttribute("message", "Booking Confirmed! Please pay at the counter.");
                ra.addFlashAttribute("token", appointment.getTokenNumber());

                return "redirect:/";
            }

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
            return "redirect:/";
        }
    }
    // Inside AppointmentController.java

    // Staff clicks this when they receive Cash at the desk
    @GetMapping("/markPaid/{id}")
    public String markPaymentAsDone(@PathVariable Long id) {
        Appointment appt = appointmentRepository.findById(id).orElse(null);
        if (appt != null) {
            appt.setPaymentStatus("PAID");
            appointmentRepository.save(appt);
        }
        return "redirect:/queue";
    }

    // --- PAYMENT ENDPOINTS ---

    @GetMapping("/payment/{id}")
    public String showPaymentPage(@PathVariable Long id, Model model) {
        model.addAttribute("appointmentId", id);
        return "payment_gateway"; // Ensure you have payment_gateway.html
    }

    @GetMapping("/payment/success/{id}")
    public String paymentSuccess(@PathVariable Long id, RedirectAttributes ra) {
        Appointment appt = appointmentRepository.findById(id).orElse(null);
        if (appt != null) {
            // FIX: Now that they paid, move them to the actual Queue
            appt.setPaymentStatus("PAID");
            appt.setStatus("WAITING");
            appointmentRepository.save(appt);

            ra.addFlashAttribute("message", "Payment Successful! You are now in the queue.");
            ra.addFlashAttribute("token", appt.getTokenNumber());
        }
        return "redirect:/";
    }

    // --- 2. DOCTOR SIDE: Dashboard & Actions ---

    @GetMapping("/queue")
    public String viewQueue(Model model) {
        model.addAttribute("waitingList", appointmentService.getWaitingQueue());
        return "queue";
    }

    @GetMapping("/start/{id}")
    public String startAppointment(@PathVariable("id") Long id) {
        try {
            appointmentService.startAppointment(id);
        } catch (Exception e) {
            System.out.println("Error starting appointment: " + e.getMessage());
        }
        return "redirect:/queue";
    }

    @GetMapping("/complete/{id}")
    public String completeAppointment(@PathVariable Long id) {
        appointmentService.completeAppointment(id);
        return "redirect:/queue";
    }

    @GetMapping("/appointments")
    public String viewAllHistory(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "appointments";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // --- 3. PUBLIC DISPLAY (TV Screen) ---

    @GetMapping("/display")
    public String showPublicDisplay(Model model) {
        Appointment current = appointmentRepository.findAll().stream()
                .filter(a -> "IN_PROGRESS".equals(a.getStatus()))
                .findFirst()
                .orElse(null);

        model.addAttribute("currentPatient", current);
        return "display";
    }
}