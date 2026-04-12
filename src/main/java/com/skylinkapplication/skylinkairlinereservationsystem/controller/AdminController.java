package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.service.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller; // Make sure it's this annotation
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/api/admin") // This creates the base path
@PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private MonitoringService monitoringService;

    // This maps to the final path: /api/admin/dashboard
    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model) {
        try {
            model.addAttribute("metrics", monitoringService.getSystemMetrics());
            logger.info("Admin dashboard accessed successfully");
            // This tells Thymeleaf to look for "admin-dashboard.html"
            return "admin-dashboard";
        } catch (Exception e) {
            logger.error("Error fetching system metrics for admin dashboard", e);
            model.addAttribute("error", "Unable to load dashboard metrics. Please try again later.");
            return "error";
        }
    }
}