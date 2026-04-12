package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/dashboard")
    public String viewBookings(@RequestParam Long userId, Model model) {
        model.addAttribute("bookings", bookingService.getBookingsByUserId(userId));
        return "booking-dashboard";
    }

    @PostMapping("/create")
    public String createBooking(@ModelAttribute BookingDTO bookingDTO, Model model) {
        BookingDTO createdBooking = bookingService.createBooking(bookingDTO);
        model.addAttribute("booking", createdBooking);
        return "redirect:/api/booking/dashboard";
    }

    @PostMapping("/cancel")
    public String cancelBooking(@RequestParam Long bookingId, Model model) {
        bookingService.cancelBooking(bookingId);
        return "redirect:/api/booking/dashboard";
    }
}