package com.example.appointment.repository;
import java.util.List;
import java.time.LocalDate;
import com.example.appointment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // JpaRepository provides save(), findAll(), deleteById(), etc. automatically.
    List<Appointment> findByPatientEmail(String patientEmail);
    boolean existsByDateAndTime(LocalDate date, String time);
    boolean existsByPatientNameAndDate(String patientName, LocalDate date);

    List<Appointment> findByStatus(String status);
    List<Appointment> findByDate(LocalDate date);

    List<Appointment> findByDateAndStatusNot(LocalDate date, String status);

}
