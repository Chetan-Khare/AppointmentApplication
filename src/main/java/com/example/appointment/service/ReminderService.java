package com.example.appointment.service;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReminderService {

    @Autowired private AppointmentRepository appointmentRepo;
    @Autowired private JavaMailSender mailSender;

    // --- 1. EXISTING: Daily Reminder (Tomorrow's Appts) ---
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepo.findByDate(tomorrow);
        for (Appointment appt : appointments) {
            sendEmail(appt.getPatientEmail(), "Reminder: Appointment Tomorrow",
                    "Don't forget your appointment tomorrow at " + appt.getTime());
        }
    }

    // --- 2. NEW: 1-Hour Before Reminder (Runs every 15 mins) ---
    @Scheduled(fixedRate = 900000)
    public void sendHourlyReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        List<Appointment> appointments = appointmentRepo.findByDate(today);

        for (Appointment appt : appointments) {
            LocalTime apptTime = LocalTime.parse(appt.getTime());
            // Check if appointment is in ~1 hour (between 50-70 mins from now)
            if (apptTime.minusHours(1).isBefore(now.plusMinutes(10)) &&
                    apptTime.minusHours(1).isAfter(now.minusMinutes(10))) {

                sendEmail(appt.getPatientEmail(), "Urgent: Appointment in 1 Hour",
                        "Your appointment with " + appt.getDoctorName() + " is in 1 hour (" + appt.getTime() + ").");
            }
        }
    }

    // --- 3. PUBLIC METHODS (For Controller to use) ---

    public void sendBookingConfirmation(Appointment appt) {
        sendEmail(appt.getPatientEmail(), "Booking Confirmed ✅",
                "Hello " + appt.getPatientName() + ",\nYour appointment is confirmed for "
                        + appt.getDate() + " at " + appt.getTime() + ".\nToken: " + appt.getTokenNumber());
    }

    public void sendPrescription(Appointment appt, String notes) {
        sendEmail(appt.getPatientEmail(), "Prescription from Dr. " + appt.getDoctorName() + " 💊",
                "Here is your prescription:\n\n" + notes + "\n\nGet well soon!");
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("📧 Email sent to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }
}