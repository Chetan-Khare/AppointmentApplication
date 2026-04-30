package com.example.appointment.controller;

import com.example.appointment.model.Appointment;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.service.AppointmentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PaymentController.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${clinic.consultation-fee:500}")
    private int consultationFee;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @PostMapping(value = "/create-order/{publicId}", produces = "application/json")
    public String createOrder(@PathVariable String publicId) throws RazorpayException {
        logger.info("Creating Razorpay order for publicId: {}", publicId);
        
        if (keyId == null || keyId.equals("YOUR_KEY_ID") || keySecret == null || keySecret.equals("YOUR_KEY_SECRET")) {
            logger.error("Razorpay keys are not configured correctly! KeyId: {}", keyId);
            throw new RuntimeException("Razorpay keys are not configured. Please set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables.");
        }

        Appointment appt = appointmentRepository.findByPublicId(publicId);
        if (appt == null) {
            logger.error("Appointment not found for publicId: {}", publicId);
            throw new RuntimeException("Appointment not found");
        }
        
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", consultationFee * 100); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + appt.getId());

            Order order = client.orders.create(orderRequest);
            logger.info("Order created successfully: {}", order.get("id").toString());
            return order.toString();
        } catch (Exception e) {
            logger.error("Error creating Razorpay order: {}", e.getMessage());
            throw e;
        }
    }
}
