package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private MonitoringService monitoringService;

    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        model.addAttribute("metrics", monitoringService.getSystemMetrics());
        return "admin-dashboard";
    }
}