package com.example.appointment.service;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AppointmentService {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @org.springframework.beans.factory.annotation.Value("${clinic.consultation-fee:500}")
    private int consultationFee;

    public List<Appointment> getAppointmentsForAdmin(String doctorSearch) {
        if (doctorSearch != null && !doctorSearch.isEmpty()) {
            return appointmentRepository.findByDoctorNameContainingIgnoreCase(doctorSearch);
        }
        return appointmentRepository.findAll();
    }

    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        LocalDate today = LocalDate.now();

        stats.put("totalAppointments", appointmentRepository.count());
        stats.put("waitingToday", appointmentRepository.countByDateAndStatus(today, "WAITING"));
        stats.put("completedToday", appointmentRepository.countByDateAndStatus(today, "COMPLETED"));
        
        long paidTodayCount = appointmentRepository.countPaidAppointmentsByDate(today);
        stats.put("todaysRevenue", paidTodayCount * consultationFee); 

        return stats;
    }

    public Map<String, Object> getChartData() {
        Map<String, Object> chartData = new java.util.HashMap<>();
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
        
        List<Object[]> statsDate = appointmentRepository.countAppointmentsByDate(sevenDaysAgo);
        Map<String, Long> dateMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            dateMap.put(sevenDaysAgo.plusDays(i).toString(), 0L);
        }
        for (Object[] row : statsDate) {
            dateMap.put(row[0].toString(), (Long) row[1]);
        }
        chartData.put("dates", dateMap.keySet());
        chartData.put("counts", dateMap.values());

        List<Object[]> statsMode = appointmentRepository.countAppointmentsByPaymentMode();
        List<String> modes = new ArrayList<>();
        List<Long> modeCounts = new ArrayList<>();
        for (Object[] row : statsMode) {
            modes.add(row[0] != null ? row[0].toString() : "Unknown");
            modeCounts.add((Long) row[1]);
        }
        chartData.put("modes", modes);
        chartData.put("modeCounts", modeCounts);

        return chartData;
    }

    public List<Appointment> getDoctorAppointments(String doctorName) {
        return appointmentRepository.findByDoctorNameIgnoreCaseAndPaymentStatusInAndStatusIn(
                doctorName.trim(), List.of("PAID", "PAY_AT_CLINIC"), List.of("WAITING", "IN_PROGRESS"));
    }

    public void startAppointment(Long id, String cabin) {
        appointmentRepository.findByStatus("IN_PROGRESS").forEach(a -> {
            a.setStatus("COMPLETED");
            appointmentRepository.save(a);
        });

        Appointment current = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        current.setStatus("IN_PROGRESS");
        appointmentRepository.save(current);

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("tokenNumber", current.getTokenNumber());
        payload.put("patientName", current.getPatientName());
        payload.put("cabin", cabin);
        messagingTemplate.convertAndSend("/topic/appointment", payload);
        
        logger.info("Started appointment for patient: {} in cabin: {}", current.getPatientName(), cabin);
    }

    public void bookAppointment(Appointment appointment) throws Exception {
        if (appointmentRepository.existsByDateAndTime(appointment.getDate(), appointment.getTime())) {
            throw new Exception("This time slot is already booked.");
        }

        long count = appointmentRepository.count();
        appointment.setTokenNumber((int) count + 1);

        if ("ONLINE".equalsIgnoreCase(appointment.getPaymentMode())) {
            appointment.setStatus("PENDING_PAYMENT");
            appointment.setPaymentStatus("PENDING");
        } else {
            appointment.setStatus("WAITING");
            appointment.setPaymentStatus("PAY_AT_CLINIC");
        }
        // 3. Set Public UUID for secure links
        appointment.setPublicId(UUID.randomUUID().toString());

        // 4. Save
        appointmentRepository.save(appointment);
        logger.info("New appointment booked: Token #{}", appointment.getTokenNumber());
    }

    public void completeAppointment(Long id, String notes) {
        Appointment appt = appointmentRepository.findById(id).orElseThrow();
        appt.setPrescription(notes);
        appt.setStatus("COMPLETED");
        appointmentRepository.save(appt);
        logger.info("Appointment #{} completed", id);
    }
    
    public void markAsPaid(Long id) {
        appointmentRepository.findById(id).ifPresent(appt -> {
            appt.setPaymentStatus("PAID");
            appointmentRepository.save(appt);
            logger.info("Appointment #{} marked as PAID", id);
        });
    }

    public void cancelAppointment(Long id) {
        appointmentRepository.findById(id).ifPresent(appt -> {
            appt.setStatus("CANCELLED");
            appointmentRepository.save(appt);
            logger.info("Appointment #{} CANCELLED", id);
        });
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
}