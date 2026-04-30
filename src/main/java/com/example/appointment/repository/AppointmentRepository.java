package com.example.appointment.repository;
import java.util.List;
import java.time.LocalDate;
import com.example.appointment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // JpaRepository provides save(), findAll(), deleteById(), etc. automatically.

    boolean existsByDateAndTime(LocalDate date, String time);
    boolean existsByPatientNameAndDate(String patientName, LocalDate date);
    List<Appointment> findByPatientEmail(String patientEmail);
    List<Appointment> findByDateAndDoctorName(LocalDate date, String doctorName);

    List<Appointment> findByDate(LocalDate date);
    List<Appointment> findByStatus(String status);
    List<Appointment> findByDateAndStatus(LocalDate date, String status);
    
    List<Appointment> findByDoctorNameContainingIgnoreCase(String doctorName);

    Appointment findByPublicId(String publicId);
    
    long countByDateAndStatus(LocalDate date, String status);
    
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.date = :date AND a.paymentStatus = 'PAID'")
    long countPaidAppointmentsByDate(@Param("date") LocalDate date);

    List<Appointment> findByStatusIn(List<String> statuses);

    List<Appointment> findByDoctorNameIgnoreCaseAndPaymentStatusInAndStatusIn(String doctorName, List<String> paymentStatuses, List<String> statuses);

    // 1. Get count of appointments for each day (Used for Bar Chart)
    @Query("SELECT a.date, COUNT(a) FROM Appointment a WHERE a.date >= :startDate GROUP BY a.date ORDER BY a.date ASC")
    List<Object[]> countAppointmentsByDate(@Param("startDate") LocalDate startDate);

    // 2. Get count by Payment Mode (Used for Doughnut Chart)
    @Query("SELECT a.paymentMode, COUNT(a) FROM Appointment a GROUP BY a.paymentMode")
    List<Object[]> countAppointmentsByPaymentMode();
}
