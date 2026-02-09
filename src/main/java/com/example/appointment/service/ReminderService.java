package com.example.appointment.service;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
@Service
public class ReminderService {

    @Autowired private AppointmentRepository appointmentRepo;
    @Autowired private JavaMailSender mailSender;

    // Cron: "0 0 8 * * ?" means Every day at 8:00:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendAppointmentReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Find all appointments for tomorrow
        List<Appointment> appointments = appointmentRepo.findByDate(tomorrow);

        for (Appointment appt : appointments) {
            sendEmail(appt);
        }
    }

    private void sendEmail(Appointment appt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(appt.getPatientEmail());
        message.setSubject("Reminder: Your Appointment Tomorrow");
        message.setText("Dear " + appt.getPatientName() + ",\n\n" +
                "This is a reminder for your appointment with " + appt.getDoctorName() +
                " scheduled for tomorrow at " + appt.getTime() + ".\n\n" +
                "See you then!");

        mailSender.send(message);
    }
}