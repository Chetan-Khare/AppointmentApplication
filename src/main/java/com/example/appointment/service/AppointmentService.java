package com.example.appointment.service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.example.appointment.model.Appointment;
import java.util.stream.Collectors;
import com.example.appointment.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {
    public void recallPatient(Long id) {
        Appointment current = appointmentRepository.findById(id).orElse(null);
        if (current != null) {
            // Just broadcast again! This triggers the display.html announcement logic
            messagingTemplate.convertAndSend("/topic/appointment", current);
        }
    }

    // Inject the Repository to talk to the database
    @Autowired
    private AppointmentRepository appointmentRepository;
    public void bookAppointment(Appointment appointment) throws Exception {
        // 1. Validation: Check for Time Slot Collision
        if (appointmentRepository.existsByDateAndTime(appointment.getDate(), appointment.getTime())) {
            throw new Exception("This time slot is already booked. Please choose another.");
        }

        // 2. Validation: Check for Duplicate Name on Same Day
        if (appointmentRepository.existsByPatientNameAndDate(appointment.getPatientName(), appointment.getDate())) {
            throw new Exception("You already have an appointment booked for this day.");
        }

        // --- MISSING LOGIC ADDED HERE ---
        // 3. Token Generation: Count existing records and add 1
        long count = appointmentRepository.count();
        appointment.setTokenNumber((int) count + 1);

        // 4. Status Initialization: Ensure it starts as WAITING
        appointment.setStatus("WAITING");
        // --------------------------------

        // 5. Save the appointment with the new Token and Status
        appointmentRepository.save(appointment);
    }
    public List<Appointment> getWaitingQueue() {
        return appointmentRepository.findAll().stream()
                .filter(a -> "WAITING".equals(a.getStatus()) || "IN_PROGRESS".equals(a.getStatus()))
                .toList();
    }
    public List<Appointment> getCompletedAppointments() {
        return appointmentRepository.findAll().stream()
                .filter(a -> "COMPLETED".equals(a.getStatus()))
                .collect(Collectors.toList());
    }

    public void completeAppointment(Long id) {
        Appointment a =  appointmentRepository.findById(id).orElseThrow();
        a.setStatus("COMPLETED");
        appointmentRepository.save(a);

    }

    @Autowired
    private SimpMessagingTemplate messagingTemplate; // Inject the "shouter"

    public void startAppointment(Long id) {
        // 1. Complete any previous patient
        List<Appointment> currentlyActive = appointmentRepository.findByStatus("IN_PROGRESS");
        for (Appointment active : currentlyActive) {
            active.setStatus("COMPLETED");
            appointmentRepository.save(active);
        }

        // 2. Start the NEW patient (Crucial step!)
        Appointment current = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        current.setStatus("IN_PROGRESS"); // Mark them as the one currently with the doctor
        appointmentRepository.save(current);

        // 3. BROADCAST: Tell the WebSocket display who the new patient is
        messagingTemplate.convertAndSend("/topic/appointment", current);

        System.out.println("Broadcasting Token: " + current.getTokenNumber());
    }



    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll(); // Fetches all from Database
    }

    // Changed 'int' to 'Long' to match the database ID type
    public boolean cancelAppointment(Long id) {
        if (appointmentRepository.existsById(id)) {
            appointmentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}