package com.example.appointment.service;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {
    public void startAppointment(Long id) {
        // 1. Find the person who is currently 'IN_PROGRESS' and mark them 'COMPLETED'
        List<Appointment> currentlyActive = appointmentRepository.findByStatus("IN_PROGRESS");
        for (Appointment active : currentlyActive) {
            active.setStatus("COMPLETED");
            appointmentRepository.save(active);
        }

        // 2. Now, take the new patient and set them to 'IN_PROGRESS'
        Appointment nextPatient = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        nextPatient.setStatus("IN_PROGRESS");
        appointmentRepository.save(nextPatient);
    }

    // Inject the Repository to talk to the database
    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Appointment> getWaitingQueue() {
        return appointmentRepository.findAll().stream()
                .filter(a -> "WAITING".equals(a.getStatus()))
                .toList();
    }


    public void completeAppointment(Long id) {
        Appointment a =  appointmentRepository.findById(id).orElseThrow();
        a.setStatus("COMPLETED");
        appointmentRepository.save(a);
    }

    public Appointment bookAppointment(String name, String date, String time) {
        // ID is now handled automatically by @GeneratedValue in the Model
        Appointment ap = new Appointment(null, name, date, time);
        ap.setPatientName(name);
        ap.setDate(date);
        ap.setTime(time);

        // Calculate token: total appointments today + 1
        long count = appointmentRepository.count();
        ap.setTokenNumber((int) count + 1);

        return appointmentRepository.save(ap);
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