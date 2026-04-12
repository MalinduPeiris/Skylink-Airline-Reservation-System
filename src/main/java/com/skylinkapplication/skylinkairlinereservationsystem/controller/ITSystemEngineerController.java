package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.*;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.SupportTicket;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.SupportTicketRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Calendar;
import java.util.Locale;

@Controller
@RequestMapping("/dashboard/it-system-engineer")
@PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
public class ITSystemEngineerController {

    private static final Logger logger = LoggerFactory.getLogger(ITSystemEngineerController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private SupportService supportService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FlightService flightService;

    // Main dashboard page
    @GetMapping("/dashboard")
    public String showITSystemEngineerDashboard(Model model) {
        try {
            // Fetch metrics data
            long totalUsers = userRepository.count();
            long frequentTravelers = userRepository.findByRole(Role.FREQUENT_TRAVELER).size();
            long activeBookings = bookingRepository.findByStatus(Booking.Status.CONFIRMED).size();
            long pendingTickets = supportTicketRepository.findByStatus(SupportTicket.Status.OPEN).size();
            
            // Fetch recent activity data
            List<User> recentUsers = userRepository.findAll();
            // Get last 5 users (simplified approach)
            List<User> lastUsers = recentUsers.size() > 5 ? 
                recentUsers.subList(Math.max(0, recentUsers.size() - 5), recentUsers.size()) : 
                recentUsers;
            
            List<Booking> recentBookings = bookingRepository.findAll();
            // Get last 5 bookings (simplified approach)
            List<Booking> lastBookings = recentBookings.size() > 5 ? 
                recentBookings.subList(Math.max(0, recentBookings.size() - 5), recentBookings.size()) : 
                recentBookings;
            
            // Prepare booking trend data (last 6 months)
            List<Integer> bookingTrends = new ArrayList<>();
            List<String> monthLabels = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            
            // Generate data for the last 6 months
            for (int i = 5; i >= 0; i--) {
                Calendar monthCal = (Calendar) cal.clone();
                monthCal.add(Calendar.MONTH, -i);
                
                // Get month name
                String monthName = monthCal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
                monthLabels.add(monthName);
                
                // Get start and end of month
                Calendar startCal = (Calendar) monthCal.clone();
                startCal.set(Calendar.DAY_OF_MONTH, 1);
                startCal.set(Calendar.HOUR_OF_DAY, 0);
                startCal.set(Calendar.MINUTE, 0);
                startCal.set(Calendar.SECOND, 0);
                startCal.set(Calendar.MILLISECOND, 0);
                
                Calendar endCal = (Calendar) monthCal.clone();
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                endCal.set(Calendar.HOUR_OF_DAY, 23);
                endCal.set(Calendar.MINUTE, 59);
                endCal.set(Calendar.SECOND, 59);
                endCal.set(Calendar.MILLISECOND, 999);
                
                Date startOfMonth = startCal.getTime();
                Date endOfMonth = endCal.getTime();
                
                // Count bookings in this month
                long count = recentBookings.stream()
                    .filter(booking -> !booking.getBookingDate().before(startOfMonth) && !booking.getBookingDate().after(endOfMonth))
                    .count();
                
                bookingTrends.add((int) count);
            }
            
            // Add data to model
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("frequentTravelers", frequentTravelers);
            model.addAttribute("activeBookings", activeBookings);
            model.addAttribute("pendingTickets", pendingTickets);
            model.addAttribute("systemAlerts", 0); // Placeholder
            model.addAttribute("recentUsers", lastUsers);
            model.addAttribute("recentBookings", lastBookings);
            model.addAttribute("bookingTrends", bookingTrends);
            model.addAttribute("monthLabels", monthLabels);
            
            logger.info("IT System Engineer dashboard accessed");
            return "admin-dashboard";
        } catch (Exception e) {
            logger.error("Error loading IT System Engineer dashboard", e);
            model.addAttribute("error", "Unable to load dashboard. Please try again later.");
            return "error";
        }
    }

    // User Management Tab
    @GetMapping("/user-management")
    public String showUserManagement(Model model) {
        try {
            List<User> allUsers = userRepository.findAll();
            
            // Categorize users
            List<User> frequentTravelers = allUsers.stream()
                    .filter(user -> user.getRole() == Role.FREQUENT_TRAVELER)
                    .collect(Collectors.toList());
            
            List<User> admins = allUsers.stream()
                    .filter(user -> user.getRole() != Role.FREQUENT_TRAVELER)
                    .collect(Collectors.toList());
            
            model.addAttribute("allUsers", allUsers);
            model.addAttribute("frequentTravelers", frequentTravelers);
            model.addAttribute("admins", admins);
            model.addAttribute("bookingRepository", bookingRepository);
            
            logger.info("User management page accessed with {} total users", allUsers.size());
            return "user-management-dashboard";
        } catch (Exception e) {
            logger.error("Error loading user management page", e);
            model.addAttribute("error", "Unable to load user management data. Please try again later.");
            return "error";
        }
    }

    // Update user
    @PostMapping("/user/update")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String username,
                             @RequestParam String email,
                             @RequestParam(required = false) String phonenumber,
                             @RequestParam(required = false) String address,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Check if username or email already exists for another user
            if (!user.getUsername().equals(username) && userRepository.findByUsername(username).isPresent()) {
                throw new RuntimeException("Username already exists");
            }
            
            if (!user.getEmail().equals(email) && userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("Email already exists");
            }
            
            user.setUsername(username);
            user.setEmail(email);
            user.setPhonenumber(phonenumber);
            user.setAddress(address);
            user.setRole(Role.valueOf(role));
            
            userRepository.save(user);
            
            logger.info("User updated successfully: ID {}", id);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
            return "redirect:/dashboard/it-system-engineer/user-management";
        } catch (Exception e) {
            logger.error("Error updating user: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/user-management";
        }
    }

    // Delete user
    @PostMapping("/user/delete")
    public String deleteUser(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            // Check if user has bookings
            List<Booking> userBookings = bookingRepository.findByUser_Id(id);
            if (!userBookings.isEmpty()) {
                throw new RuntimeException("Cannot delete user with existing bookings. Please cancel bookings first.");
            }
            
            userRepository.deleteById(id);
            logger.info("User deleted successfully: ID {}", id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully!");
            return "redirect:/dashboard/it-system-engineer/user-management";
        } catch (Exception e) {
            logger.error("Error deleting user: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/user-management";
        }
    }

    // Create user
    @PostMapping("/user/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam(required = false) String phonenumber,
                             @RequestParam(required = false) String address,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {
        try {
            // Check if username or email already exists
            if (userRepository.findByUsername(username).isPresent()) {
                throw new RuntimeException("Username already exists");
            }
            
            if (userRepository.findByEmail(email).isPresent()) {
                throw new RuntimeException("Email already exists");
            }
            
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setPhonenumber(phonenumber);
            user.setAddress(address);
            user.setRole(Role.valueOf(role));
            
            userRepository.save(user);
            
            logger.info("User created successfully: ID {}", user.getId());
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
            return "redirect:/dashboard/it-system-engineer/user-management";
        } catch (Exception e) {
            logger.error("Error creating user", e);
            redirectAttributes.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/user-management";
        }
    }

    // Promotions Tab - Show promotions
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'MARKETING_EXECUTIVE')")
    @GetMapping("/promotions")
    public String showPromotions(Model model) {
        try {
            List<PromotionDTO> promotions = promotionService.getAllPromotions();
            model.addAttribute("promotions", promotions);
            logger.info("IT System Engineer accessing promotions dashboard with {} promotions", promotions.size());
            return "promotion-manager-dashboard";
        } catch (Exception e) {
            logger.error("Error loading promotions for IT System Engineer", e);
            model.addAttribute("error", "Unable to load promotions: " + e.getMessage());
            model.addAttribute("promotions", Collections.emptyList());
            return "promotion-manager-dashboard";
        }
    }

    // View promotion details
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'MARKETING_EXECUTIVE')")
    @GetMapping("/promotions/{id}")
    public String viewPromotion(@PathVariable Long id, Model model) {
        try {
            PromotionDTO promotion = promotionService.getPromotionById(id);
            model.addAttribute("promotion", promotion);
            logger.info("IT System Engineer viewing promotion details: ID {}", id);
            return "promotion-details";
        } catch (RuntimeException e) {
            logger.error("Error retrieving promotion with ID: {}", id, e);
            model.addAttribute("error", "Unable to load promotion details. Please try again.");
            return "error";
        }
    }

    // Show edit promotion form
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'MARKETING_EXECUTIVE')")
    @GetMapping("/promotions/{id}/edit")
    public String editPromotionForm(@PathVariable Long id, Model model) {
        try {
            PromotionDTO promotion = promotionService.getPromotionById(id);
            model.addAttribute("promotion", promotion);
            logger.info("IT System Engineer editing promotion: ID {}", id);
            return "promotion-form";
        } catch (RuntimeException e) {
            logger.error("Error retrieving promotion for editing with ID: {}", id, e);
            model.addAttribute("error", "Unable to load promotion for editing. Please try again.");
            return "error";
        }
    }

    // Create promotion
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'MARKETING_EXECUTIVE')")
    @PostMapping("/promotions/create")
    public String createPromotion(@RequestParam("targetCriteria") String targetCriteria,
                                  @RequestParam("discount") Double discount,
                                  @RequestParam("promoCode") String promoCode,
                                  @RequestParam("validityStart") String validityStartStr,
                                  @RequestParam("validityEnd") String validityEndStr,
                                  RedirectAttributes redirectAttributes) {
        try {
            logger.info("IT System Engineer creating promotion with data: targetCriteria={}, discount={}, promoCode={}, validityStart={}, validityEnd={}",
                    targetCriteria, discount, promoCode, validityStartStr, validityEndStr);

            // Parse dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date validityStart = dateFormat.parse(validityStartStr);
            Date validityEnd = dateFormat.parse(validityEndStr);

            // Validate input
            if (targetCriteria == null || targetCriteria.trim().isEmpty() ||
                    discount == null || discount <= 0 ||
                    promoCode == null || promoCode.trim().isEmpty()) {
                logger.warn("Invalid promotion data provided for creation");
                redirectAttributes.addFlashAttribute("error", "Target criteria, promo code, and discount (positive value) are required.");
                return "redirect:/dashboard/it-system-engineer/promotions";
            }

            // Create DTO with the form data
            PromotionDTO promotionDTO = new PromotionDTO();
            promotionDTO.setTargetCriteria(targetCriteria);
            promotionDTO.setDiscount(discount);
            promotionDTO.setPromoCode(promoCode);
            promotionDTO.setValidityStart(validityStart);
            promotionDTO.setValidityEnd(validityEnd);

            PromotionDTO createdPromotion = promotionService.createPromotion(promotionDTO);
            logger.info("Promotion created successfully: {}", createdPromotion.getDiscount());
            redirectAttributes.addFlashAttribute("success", "Promotion created successfully.");
            return "redirect:/dashboard/it-system-engineer/promotions";
        } catch (ParseException e) {
            logger.error("Error parsing dates for promotion creation", e);
            redirectAttributes.addFlashAttribute("error", "Invalid date format. Please use YYYY-MM-DD format.");
            return "redirect:/dashboard/it-system-engineer/promotions";
        } catch (RuntimeException e) {
            logger.error("Error creating promotion", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/dashboard/it-system-engineer/promotions";
        }
    }

    // Update promotion
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'MARKETING_EXECUTIVE')")
    @PostMapping("/promotions/update/{id}")
    public String updatePromotion(@PathVariable Long id,
                                  @RequestParam("targetCriteria") String targetCriteria,
                                  @RequestParam("discount") Double discount,
                                  @RequestParam("promoCode") String promoCode,
                                  @RequestParam("validityStart") String validityStartStr,
                                  @RequestParam("validityEnd") String validityEndStr,
                                  RedirectAttributes redirectAttributes) {
        try {
            logger.info("IT System Engineer updating promotion ID {}: targetCriteria={}, discount={}, promoCode={}, validityStart={}, validityEnd={}",
                    id, targetCriteria, discount, promoCode, validityStartStr, validityEndStr);

            // Parse dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date validityStart = dateFormat.parse(validityStartStr);
            Date validityEnd = dateFormat.parse(validityEndStr);

            // Create DTO with the form data
            PromotionDTO updatedPromotionDTO = new PromotionDTO();
            updatedPromotionDTO.setId(id);
            updatedPromotionDTO.setTargetCriteria(targetCriteria);
            updatedPromotionDTO.setDiscount(discount);
            updatedPromotionDTO.setPromoCode(promoCode);
            updatedPromotionDTO.setValidityStart(validityStart);
            updatedPromotionDTO.setValidityEnd(validityEnd);

            PromotionDTO updatedPromotion = promotionService.updatePromotion(id, updatedPromotionDTO);
            logger.info("Promotion updated successfully: ID {}", id);
            redirectAttributes.addFlashAttribute("success", "Promotion updated successfully.");
            return "redirect:/dashboard/it-system-engineer/promotions";
        } catch (ParseException e) {
            logger.error("Error parsing dates for promotion update with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Invalid date format. Please use YYYY-MM-DD format.");
            return "redirect:/dashboard/it-system-engineer/promotions";
        } catch (RuntimeException e) {
            logger.error("Error updating promotion with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Unable to update promotion. Please try again.");
            return "redirect:/dashboard/it-system-engineer/promotions";
        }
    }

    // Delete promotion
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'MARKETING_EXECUTIVE')")
    @PostMapping("/promotions/delete/{id}")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            logger.info("Promotion deleted successfully: ID {}", id);
            redirectAttributes.addFlashAttribute("success", "Promotion deleted successfully.");
            return "redirect:/dashboard/it-system-engineer/promotions";
        } catch (RuntimeException e) {
            logger.error("Error deleting promotion with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Unable to delete promotion. Please try again.");
            return "redirect:/dashboard/it-system-engineer/promotions";
        }
    }

    // Support Tickets Tab
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'CUSTOMER_SUPPORT_OFFICER')")
    @GetMapping("/support-tickets")
    public String showSupportTickets(Model model) {
        try {
            // Fetch all data
            List<FeedbackDTO> feedbacks = feedbackService.getAllFeedbacks();
            List<SupportTicketDTO> tickets = supportService.getAllTickets();
            List<PaymentDTO> payments = paymentService.getAllPayments();
            List<BookingDTO> bookings = bookingService.getAllBookings();

            // Calculate statistics
            long feedbackCount = feedbacks.size();
            long ticketCount = tickets.size();
            long resolvedCount = tickets.stream()
                    .filter(t -> "RESOLVED".equals(t.getStatus()))
                    .count();
            long pendingCount = ticketCount - resolvedCount;

            // Add all attributes to model
            model.addAttribute("feedbacks", feedbacks);
            model.addAttribute("tickets", tickets);
            model.addAttribute("payments", payments);
            model.addAttribute("bookings", bookings);
            model.addAttribute("feedbackCount", feedbackCount);
            model.addAttribute("ticketCount", ticketCount);
            model.addAttribute("resolvedCount", resolvedCount);
            model.addAttribute("pendingCount", pendingCount);

            logger.info("IT System Engineer accessing support tickets dashboard");
            return "customer-support-manage-dashboard";
        } catch (Exception e) {
            logger.error("Error loading support tickets for IT System Engineer", e);
            model.addAttribute("error", "Unable to load support tickets. Please try again.");
            return "error";
        }
    }

    // Finance Tab - Main Dashboard
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @GetMapping("/finance")
    public String showFinance(Model model) {
        try {
            logger.info("IT System Engineer accessing finance dashboard");

            // Get all payments for dashboard overview
            List<PaymentDTO> payments = paymentService.getAllPayments();

            // Calculate statistics
            long totalPayments = payments != null ? payments.size() : 0;
            long completedPayments = payments != null ? payments.stream()
                    .filter(p -> "COMPLETED".equals(p.getStatus()))
                    .count() : 0;
            long refundedPayments = payments != null ? payments.stream()
                    .filter(p -> "REFUNDED".equals(p.getStatus()))
                    .count() : 0;
            long failedPayments = payments != null ? payments.stream()
                    .filter(p -> "FAILED".equals(p.getStatus()))
                    .count() : 0;

            // Calculate total revenue (completed payments only)
            double totalRevenue = payments != null ? payments.stream()
                    .filter(p -> "COMPLETED".equals(p.getStatus()))
                    .mapToDouble(PaymentDTO::getAmount)
                    .sum() : 0.0;

            // Calculate total refunded amount
            double totalRefunded = payments != null ? payments.stream()
                    .filter(p -> "REFUNDED".equals(p.getStatus()))
                    .mapToDouble(PaymentDTO::getAmount)
                    .sum() : 0.0;

            // Add attributes to model
            model.addAttribute("payments", payments != null ? payments : Collections.emptyList());
            model.addAttribute("totalPayments", totalPayments);
            model.addAttribute("completedPayments", completedPayments);
            model.addAttribute("refundedPayments", refundedPayments);
            model.addAttribute("failedPayments", failedPayments);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("totalRefunded", totalRefunded);

            logger.info("IT System Engineer finance dashboard loaded successfully with {} payments", totalPayments);

            return "finance-dashboard";
        } catch (Exception e) {
            logger.error("Error loading finance dashboard for IT System Engineer", e);
            model.addAttribute("error", "Unable to load finance dashboard: " + e.getMessage());
            model.addAttribute("payments", Collections.emptyList());
            model.addAttribute("totalPayments", 0);
            model.addAttribute("completedPayments", 0);
            model.addAttribute("refundedPayments", 0);
            model.addAttribute("failedPayments", 0);
            model.addAttribute("totalRevenue", 0.0);
            model.addAttribute("totalRefunded", 0.0);
            return "finance-dashboard";
        }
    }

    // Finance Sub-tab - All Payments
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @GetMapping("/finance/all-payments")
    public String showAllFinancePayments(Model model) {
        try {
            List<PaymentDTO> allPayments = paymentService.getAllPayments();
            model.addAttribute("payments", allPayments);
            model.addAttribute("activeTab", "all");

            logger.info("IT System Engineer loaded {} payments in all-payments tab", allPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading all payments for IT System Engineer", e);
            model.addAttribute("error", "Unable to load payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "all");
            return "finance-payments-tab";
        }
    }

    // Finance Sub-tab - Completed Payments
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @GetMapping("/finance/completed")
    public String showCompletedFinancePayments(Model model) {
        try {
            List<PaymentDTO> completedPayments = paymentService.getPaymentsByStatus("COMPLETED");
            model.addAttribute("payments", completedPayments);
            model.addAttribute("activeTab", "completed");

            logger.info("IT System Engineer loaded {} completed payments", completedPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading completed payments for IT System Engineer", e);
            model.addAttribute("error", "Unable to load completed payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "completed");
            return "finance-payments-tab";
        }
    }

    // Finance Sub-tab - Failed Payments
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @GetMapping("/finance/failed")
    public String showFailedFinancePayments(Model model) {
        try {
            List<PaymentDTO> failedPayments = paymentService.getPaymentsByStatus("FAILED");
            model.addAttribute("payments", failedPayments);
            model.addAttribute("activeTab", "failed");

            logger.info("IT System Engineer loaded {} failed payments", failedPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading failed payments for IT System Engineer", e);
            model.addAttribute("error", "Unable to load failed payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "failed");
            return "finance-payments-tab";
        }
    }

    // Finance Sub-tab - Refunded Payments
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @GetMapping("/finance/refunded")
    public String showRefundedFinancePayments(Model model) {
        try {
            List<PaymentDTO> refundedPayments = paymentService.getPaymentsByStatus("REFUNDED");
            model.addAttribute("payments", refundedPayments);
            model.addAttribute("activeTab", "refunded");

            logger.info("IT System Engineer loaded {} refunded payments", refundedPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading refunded payments for IT System Engineer", e);
            model.addAttribute("error", "Unable to load refunded payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "refunded");
            return "finance-payments-tab";
        }
    }

    // Finance Sub-tab - Cancelled Bookings
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @GetMapping("/finance/cancelled-bookings")
    public String showCancelledFinanceBookings(Model model) {
        try {
            List<BookingDTO> cancelledBookings = bookingService.getBookingsByStatus("CANCELLED");

            // Enrich bookings with payment information
            for (BookingDTO booking : cancelledBookings) {
                if (booking.getPaymentId() != null) {
                    try {
                        PaymentDTO payment = paymentService.getPaymentById(booking.getPaymentId());
                        booking.setPayment(payment);
                    } catch (Exception e) {
                        logger.warn("Could not fetch payment for booking {}", booking.getId());
                    }
                }
            }

            model.addAttribute("bookings", cancelledBookings);
            model.addAttribute("activeTab", "cancelled");

            logger.info("IT System Engineer loaded {} cancelled bookings", cancelledBookings.size());
            return "finance-cancelled-bookings";
        } catch (Exception e) {
            logger.error("Error loading cancelled bookings for IT System Engineer", e);
            model.addAttribute("error", "Unable to load cancelled bookings: " + e.getMessage());
            model.addAttribute("bookings", List.of());
            model.addAttribute("activeTab", "cancelled");
            return "finance-cancelled-bookings";
        }
    }

    // Finance - Create Payment
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @PostMapping("/finance/payment/create")
    public String createFinancePayment(
            @RequestParam Long bookingId,
            @RequestParam Double amount,
            @RequestParam String status,
            @RequestParam String activeTab,
            RedirectAttributes redirectAttributes) {
        try {
            if (bookingId == null || bookingId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid booking ID");
                return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
            }
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
            }

            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid status");
                return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
            }

            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setBookingId(bookingId);
            paymentDTO.setAmount(amount);
            paymentDTO.setStatus(status);

            PaymentDTO createdPayment = paymentService.createPayment(paymentDTO);

            logger.info("IT System Engineer created payment successfully: id={}", createdPayment.getId());
            redirectAttributes.addFlashAttribute("success",
                    "Payment #" + createdPayment.getId() + " created successfully!");

            return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
        } catch (RuntimeException e) {
            logger.error("Error creating payment by IT System Engineer", e);
            redirectAttributes.addFlashAttribute("error", "Error creating payment: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
        }
    }

    // Finance - Update Payment
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @PostMapping("/finance/payment/update/{id}")
    public String updateFinancePayment(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam String status,
            @RequestParam String transactionId,
            @RequestParam String activeTab,
            RedirectAttributes redirectAttributes) {
        try {
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
            }

            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid status");
                return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
            }

            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setAmount(amount);
            paymentDTO.setStatus(status);
            paymentDTO.setTransactionId(transactionId);

            paymentService.updatePayment(id, paymentDTO);

            logger.info("IT System Engineer updated payment successfully: id={}", id);
            redirectAttributes.addFlashAttribute("success", "Payment #" + id + " updated successfully!");

            return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
        } catch (RuntimeException e) {
            logger.error("Error updating payment by IT System Engineer: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error updating payment: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
        }
    }

    // Finance - Delete Payment
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @PostMapping("/finance/payment/delete/{id}")
    public String deleteFinancePayment(
            @PathVariable Long id,
            @RequestParam String activeTab,
            RedirectAttributes redirectAttributes) {
        try {
            paymentService.deletePayment(id);

            logger.info("IT System Engineer deleted payment successfully: id={}", id);
            redirectAttributes.addFlashAttribute("success", "Payment #" + id + " deleted successfully!");

            return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
        } catch (RuntimeException e) {
            logger.error("Error deleting payment by IT System Engineer: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting payment: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/finance/" + activeTab;
        }
    }

    // Finance - Update Payment Status from Cancelled Bookings
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @PostMapping("/finance/booking/update-payment-status/{paymentId}")
    public String updateFinancePaymentStatus(
            @PathVariable Long paymentId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid payment status");
                return "redirect:/dashboard/it-system-engineer/finance/cancelled-bookings";
            }

            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setStatus(status);

            paymentService.updatePayment(paymentId, paymentDTO);

            logger.info("IT System Engineer updated payment status successfully: id={}, new status={}", paymentId, status);
            redirectAttributes.addFlashAttribute("success",
                    "Payment #" + paymentId + " status updated to " + status + " successfully!");

            return "redirect:/dashboard/it-system-engineer/finance/cancelled-bookings";
        } catch (RuntimeException e) {
            logger.error("Error updating payment status by IT System Engineer: id={}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Error updating payment status: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/finance/cancelled-bookings";
        }
    }

    // Finance - Refund Payment from Cancelled Bookings
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'FINANCE_EXECUTIVE')")
    @PostMapping("/finance/booking/refund-payment/{paymentId}")
    public String refundFinancePaymentFromBooking(
            @PathVariable Long paymentId,
            RedirectAttributes redirectAttributes) {
        try {
            paymentService.refundPayment(paymentId);

            logger.info("IT System Engineer refunded payment successfully: id={}", paymentId);
            redirectAttributes.addFlashAttribute("success",
                    "Payment #" + paymentId + " refunded successfully!");

            return "redirect:/dashboard/it-system-engineer/finance/cancelled-bookings";
        } catch (RuntimeException e) {
            logger.error("Error refunding payment by IT System Engineer: id={}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Error refunding payment: " + e.getMessage());
            return "redirect:/dashboard/it-system-engineer/finance/cancelled-bookings";
        }
    }

    // Flight Schedule Tab (delegating to Flight Admin functionality)
    @GetMapping("/flight-schedule")
    public String showFlightSchedule(Model model) {
        try {
            return "redirect:/dashboard/flight-admin";
        } catch (Exception e) {
            logger.error("Error accessing flight schedule", e);
            model.addAttribute("error", "Unable to access flight schedule. Please try again later.");
            return "error";
        }
    }

    // Booking Management Dashboard
    @PreAuthorize("hasAnyRole('IT_SYSTEM_ENGINEER', 'RESERVATION_MANAGER')")
    @GetMapping("/booking-management")
    public String showBookingManagement(Model model) {
        try {
            // Get all bookings
            List<BookingDTO> bookings = bookingService.getAllBookings();
            
            // Get all flights
            List<FlightDTO> flights = flightService.getAllFlights();
            
            // Get all payments
            List<PaymentDTO> payments = paymentService.getAllPayments();
            
            // Get all users for dropdowns
            List<User> users = userRepository.findAll();
            
            // Add data to model
            model.addAttribute("bookings", bookings);
            model.addAttribute("flights", flights);
            model.addAttribute("payments", payments);
            model.addAttribute("users", users);
            
            logger.info("IT System Engineer accessing booking management dashboard");
            return "booking-management-dashboard-new";
        } catch (Exception e) {
            logger.error("Error loading booking management dashboard", e);
            model.addAttribute("error", "Unable to load booking management dashboard: " + e.getMessage());
            return "error";
        }
    }
}