package com.example.appointment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity // This tells Spring to create a database table named 'appointment'
public class Appointment {

    @Id // Marks this field as the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Automatically increments the ID (1, 2, 3...)
    private Long id; // Changed from int to Long (best practice for IDs)

    private String patientName;
    private String date;
    private String time;

    private String status="WAITING";
    private int tokenNumber;

    // 1. Mandatory No-Args Constructor (Hibernate needs this)
    public Appointment() {
    }

    // 2. Your existing constructor
    public Appointment(Long id, String patientName, String date, String time) {
        this.id = id;
        this.patientName = patientName;
        this.date = date;
        this.time = time;
        this.status = "WAITING";

    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // --- GETTER AND SETTER FOR TOKEN ---
    public int getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    // 3. Getters and Setters (Setters are required for the database to fill the object)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}