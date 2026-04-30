package com.example.appointment.repository;

import com.example.appointment.model.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {
    boolean existsByDoctorIdAndLeaveDate(Long doctorId, LocalDate leaveDate);
    List<DoctorLeave> findByDoctorId(Long doctorId);
    long countByLeaveDate(LocalDate leaveDate);
}