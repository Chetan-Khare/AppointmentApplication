package com.example.appointment.repository;

import com.example.appointment.model.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {

    // Check if a doctor is on leave for a specific date
    boolean existsByDoctorIdAndLeaveDate(Long doctorId, LocalDate leaveDate);

    // Find all leaves for a doctor
    List<DoctorLeave> findByDoctorId(Long doctorId);
}