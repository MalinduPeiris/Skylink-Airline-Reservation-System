package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.SupportTicketDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.FeedbackDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.SupportService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FeedbackService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.AuthService;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/api/support")
@PreAuthorize("hasRole('CUSTOMER_SUPPORT_OFFICER')")
public class SupportController {

    private static final Logger logger = LoggerFactory.getLogger(SupportController.class);

    @Autowired
    private SupportService supportService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AuthService authService;

    @GetMapping("/dashboard")
    public String customerSupportDashboard(Model model, Authentication authentication) {
        try {
            // Fetch tickets, feedbacks, payments, and bookings
            List<SupportTicketDTO> tickets = new ArrayList<>();
            List<FeedbackDTO> feedbacks = new ArrayList<>();
            List<PaymentDTO> payments = new ArrayList<>();
            List<BookingDTO> bookings = new ArrayList<>();
            
            try {
                tickets = supportService.getAllTickets();
            } catch (Exception e) {
                logger.warn("Failed to load support tickets: {}", e.getMessage());
                tickets = new ArrayList<>();
            }
            
            try {
                feedbacks = feedbackService.getAllFeedbacks();
            } catch (Exception e) {
                logger.warn("Failed to load feedbacks: {}", e.getMessage());
                feedbacks = new ArrayList<>();
            }
            
            try {
                payments = paymentService.getAllPayments();
            } catch (Exception e) {
                logger.warn("Failed to load payments: {}", e.getMessage());
                payments = new ArrayList<>();
            }
            
            try {
                bookings = bookingService.getAllBookings();
            } catch (Exception e) {
                logger.warn("Failed to load bookings: {}", e.getMessage());
                bookings = new ArrayList<>();
            }

            // Calculate dashboard counts
            long feedbackCount = feedbacks != null ? feedbacks.size() : 0;
            long ticketCount = tickets != null ? tickets.size() : 0;
            long resolvedCount = tickets != null ? tickets.stream()
                    .filter(ticket -> "RESOLVED".equals(ticket.getStatus()))
                    .count() : 0;
            long pendingCount = tickets != null ? tickets.stream()
                    .filter(ticket -> "OPEN".equals(ticket.getStatus()))
                    .count() : 0;

            // Load all users grouped by role
            List<User> allUsers = new ArrayList<>();
            try {
                allUsers = authService.getAllUsers();
            } catch (Exception e) {
                logger.warn("Failed to load users: {}", e.getMessage());
            }
            model.addAttribute("allUsers", allUsers);
            model.addAttribute("userCount", allUsers.size());

            // Add attributes to model
            model.addAttribute("tickets", tickets != null ? tickets : new ArrayList<>());
            model.addAttribute("feedbacks", feedbacks != null ? feedbacks : new ArrayList<>());
            model.addAttribute("payments", payments != null ? payments : new ArrayList<>());
            model.addAttribute("bookings", bookings != null ? bookings : new ArrayList<>());
            model.addAttribute("feedbackCount", feedbackCount);
            model.addAttribute("ticketCount", ticketCount);
            model.addAttribute("resolvedCount", resolvedCount);
            model.addAttribute("pendingCount", pendingCount);

            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                model.addAttribute("currentUser", currentUser);
            }

            logger.info("Customer support dashboard accessed successfully");
            return "customer-support-manage-dashboard";
        } catch (RuntimeException e) {
            logger.error("Error accessing customer support dashboard", e);
            model.addAttribute("error", "Unable to load dashboard. Please try again.");
            model.addAttribute("tickets", new ArrayList<>());
            model.addAttribute("feedbacks", new ArrayList<>());
            model.addAttribute("payments", new ArrayList<>());
            model.addAttribute("bookings", new ArrayList<>());
            model.addAttribute("feedbackCount", 0);
            model.addAttribute("ticketCount", 0);
            model.addAttribute("resolvedCount", 0);
            model.addAttribute("pendingCount", 0);
            return "error";
        }
    }

    @GetMapping("/init-sample-data")
    public String initSampleData(Model model) {
        try {
            supportService.createSampleTickets();
            model.addAttribute("success", "Sample data created successfully!");
            // Refresh data
            List<SupportTicketDTO> tickets = supportService.getAllTickets();
            List<FeedbackDTO> feedbacks = feedbackService.getAllFeedbacks();
            model.addAttribute("tickets", tickets);
            model.addAttribute("feedbacks", feedbacks);
            model.addAttribute("feedbackCount", feedbacks.size());
            model.addAttribute("ticketCount", tickets.size());
            model.addAttribute("resolvedCount", tickets.stream()
                    .filter(ticket -> "RESOLVED".equals(ticket.getStatus()))
                    .count());
            model.addAttribute("pendingCount", tickets.stream()
                    .filter(ticket -> "OPEN".equals(ticket.getStatus()))
                    .count());
            logger.info("Sample support tickets created successfully");
            return "customer-support-manage-dashboard";
        } catch (RuntimeException e) {
            logger.error("Error creating sample support tickets", e);
            model.addAttribute("error", "Failed to create sample data: " + e.getMessage());
            return "customer-support-manage-dashboard";
        }
    }

    @GetMapping("/tickets")
    public String viewTickets(Model model) {
        try {
            model.addAttribute("tickets", supportService.getAllTickets());
            logger.info("Support tickets retrieved successfully");
            return "user-support-ticket";
        } catch (RuntimeException e) {
            logger.error("Error retrieving support tickets", e);
            model.addAttribute("error", "Unable to load support tickets. Please try again.");
            return "error";
        }
    }

    @GetMapping("/api/tickets")
    @ResponseBody
    public List<SupportTicketDTO> getTicketsJson() {
        try {
            return supportService.getAllTickets();
        } catch (RuntimeException e) {
            logger.error("Error retrieving support tickets as JSON", e);
            return List.of();
        }
    }

    @PostMapping("/respond")
    public String respondToTicket(@RequestParam(name = "ticketId") Long ticketId, @RequestParam(name = "response") String response, Model model) {
        try {
            if (ticketId == null || ticketId <= 0 || response == null || response.trim().isEmpty()) {
                logger.warn("Invalid ticketId or response provided: ticketId={}, response={}", ticketId, response);
                model.addAttribute("error", "Valid ticket ID and response are required.");
                model.addAttribute("tickets", supportService.getAllTickets());
                return "support-tickets";
            }
            supportService.respondToTicket(ticketId, response);
            logger.info("Response submitted successfully for ticketId: {}", ticketId);
            model.addAttribute("success", "Response submitted successfully.");
            return "redirect:/api/support/tickets";
        } catch (RuntimeException e) {
            logger.error("Error responding to ticketId: {}", ticketId, e);
            model.addAttribute("error", "Unable to respond to ticket. Please try again.");
            model.addAttribute("tickets", supportService.getAllTickets());
            return "support-tickets";
        }
    }

    @PostMapping("/update/{id}")
    public String updateTicket(@PathVariable(name = "id") Long id, @ModelAttribute SupportTicketDTO ticketDTO, RedirectAttributes redirectAttributes) {
        try {
            supportService.updateSupportTicket(id, ticketDTO);
            logger.info("Ticket updated successfully for ID: {}", id);
            redirectAttributes.addFlashAttribute("success", "Ticket updated successfully.");
            return "redirect:/dashboard/customer-support";
        } catch (RuntimeException e) {
            logger.error("Error updating ticket ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Unable to update ticket. Please try again.");
            return "redirect:/dashboard/customer-support";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTicket(@PathVariable(name = "id") Long id, RedirectAttributes redirectAttributes) {
        try {
            supportService.deleteSupportTicket(id);
            logger.info("Ticket deleted successfully for ID: {}", id);
            redirectAttributes.addFlashAttribute("success", "Ticket deleted successfully.");
            return "redirect:/dashboard/customer-support";
        } catch (RuntimeException e) {
            logger.error("Error deleting ticket ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Unable to delete ticket. Please try again.");
            return "redirect:/dashboard/customer-support";
        }
    }

    // ─── USER MANAGEMENT ───────────────────────────────────────────────────────

    @PostMapping("/user/update/{id}")
    public String updateUser(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "username") String username,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "phonenumber", required = false) String phonenumber,
            @RequestParam(name = "address", required = false) String address,
            @RequestParam(name = "role") String role,
            @RequestParam(name = "password", required = false) String password,
            RedirectAttributes redirectAttributes) {
        try {
            User existing = authService.getUserById(id);
            User updated = new User();
            updated.setId(id);
            updated.setUsername(username.trim());
            updated.setEmail(email.trim());
            updated.setPhonenumber(phonenumber);
            updated.setAddress(address);
            updated.setRole(Role.valueOf(role));
            if (password != null && !password.trim().isEmpty()) {
                updated.setPassword(password.trim());
            } else {
                updated.setPassword(existing.getPassword()); // keep existing encoded password
            }
            authService.updateUser(id, updated);
            logger.info("Admin updated user ID: {}", id);
            redirectAttributes.addFlashAttribute("success", "User #" + id + " updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating user ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }
        return "redirect:/dashboard/customer-support";
    }

    @PostMapping("/user/delete/{id}")
    public String deleteUser(
            @PathVariable(name = "id") Long id,
            RedirectAttributes redirectAttributes) {
        try {
            authService.deleteUser(id);
            logger.info("Admin deleted user ID: {}", id);
            redirectAttributes.addFlashAttribute("success", "User #" + id + " deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting user ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete user: " + e.getMessage());
        }
        return "redirect:/dashboard/customer-support";
    }
}