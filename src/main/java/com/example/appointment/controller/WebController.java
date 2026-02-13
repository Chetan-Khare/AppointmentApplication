package com.example.appointment.controller;
import com.example.appointment.service.AppointmentService;
import org.springframework.stereotype.Controller;
@Controller
public class WebController {
    private final AppointmentService service;
    public WebController(AppointmentService service) {

        this.service = service;

    }
}