package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.SupportTicketDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private SupportService supportService;

    @GetMapping("/tickets")
    public String viewTickets(Model model) {
        model.addAttribute("tickets", supportService.getAllTickets());
        return "support-ticket";
    }

    @PostMapping("/respond")
    public String respondToTicket(@RequestParam Long ticketId, @RequestParam String response, Model model) {
        supportService.respondToTicket(ticketId, response);
        return "redirect:/api/support/tickets";
    }
}