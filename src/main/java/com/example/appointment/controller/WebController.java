package com.example.appointment.controller;
import com.example.appointment.service.AppointmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
public class WebController {
    private final AppointmentService service;




    public WebController(AppointmentService service) {

        this.service = service;

    }}