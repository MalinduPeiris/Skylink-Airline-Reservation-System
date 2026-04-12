package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/process")
    public String showPaymentPage(@RequestParam Long bookingId, Model model) {
        model.addAttribute("bookingId", bookingId);
        return "payment-page";
    }

    @PostMapping("/process")
    public String processPayment(@ModelAttribute PaymentDTO paymentDTO, Model model) {
        PaymentDTO processedPayment = paymentService.processPayment(paymentDTO);
        model.addAttribute("payment", processedPayment);
        return "redirect:/api/booking/dashboard";
    }

    @PostMapping("/refund")
    public String refundPayment(@RequestParam Long paymentId, Model model) {
        paymentService.refundPayment(paymentId);
        return "redirect:/api/booking/dashboard";
    }
}