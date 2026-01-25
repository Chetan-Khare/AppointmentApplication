package com.example.appointment.repository;
import java.util.List;
import com.example.appointment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // That's it! No methods needed.
    // JpaRepository provides save(), findAll(), deleteById(), etc. automatically.
    List<Appointment> findByStatus(String status);
}