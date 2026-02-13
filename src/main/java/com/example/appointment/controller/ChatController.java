package com.example.appointment.controller;

import com.example.appointment.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private AppointmentRepository repository;

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public String handleChat(String userMessage) {
        userMessage = userMessage.toLowerCase();

        // 1. Check for Queue Status (Real-time DB data)
        if (userMessage.contains("status") || userMessage.contains("many")) {
            long count = repository.findAll().stream()
                    .filter(a -> "WAITING".equals(a.getStatus()))
                    .count();
            return "Clinic Bot: There are currently " + count + " patients waiting in the queue.";
        }

        // 2. Check for Wait Times
        else if (userMessage.contains("wait")) {
            return "Clinic Bot: The average wait time today is about 15 minutes per patient.";
        }

        // 3. Check for Hours
        else if (userMessage.contains("time") || userMessage.contains("open")) {
            return "Clinic Bot: We are open from 9:00 AM to 6:00 PM.";
        }

        // 4. Greetings
        else if (userMessage.contains("hello") || userMessage.contains("hi")) {
            return "Clinic Bot: Hello! How can I help you with your appointment today?";
        }

        // 5. Help Menu
        else if (userMessage.contains("help")) {
            return "Clinic Bot: You can ask me about 'waiting status', 'opening hours', or 'average wait'.";
        }

        // 6. Default Fallback
        else {
            return "Clinic Bot: I'm not sure about that. Type 'help' to see what I can answer, or ask to speak with our receptionist!";
        }
    }
}