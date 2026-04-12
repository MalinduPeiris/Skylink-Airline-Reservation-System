package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/payment")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    /**
     * Display payment management dashboard (Finance Executive only)
     */
    @GetMapping("/manage")
    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
    public String showPaymentManagementDashboard(Model model) {
        try {
            logger.info("Finance Executive accessing payment management dashboard");
            model.addAttribute("payments", paymentService.getAllPayments());
            return "payment-management";
        } catch (Exception e) {
            logger.error("Error loading payment management dashboard", e);
            model.addAttribute("error", "Unable to load payment dashboard: " + e.getMessage());
            model.addAttribute("payments", java.util.Collections.emptyList());
            return "payment-management";
        }
    }

    /**
     * Create new payment (Finance Executive only)
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
    public String createPayment(
            @RequestParam Long bookingId,
            @RequestParam Double amount,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate inputs
            if (bookingId == null || bookingId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid booking ID");
                return "redirect:/api/payment/manage";
            }
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/api/payment/manage";
            }
            if (status == null || status.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Status is required");
                return "redirect:/api/payment/manage";
            }

            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid status. Must be COMPLETED or FAILED");
                return "redirect:/api/payment/manage";
            }

            // Create PaymentDTO
            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setBookingId(bookingId);
            paymentDTO.setAmount(amount);
            paymentDTO.setStatus(status);

            // Process payment
            PaymentDTO createdPayment = paymentService.createPayment(paymentDTO);

            logger.info("Payment created successfully: id={}, bookingId={}, amount={}",
                    createdPayment.getId(), bookingId, amount);

            redirectAttributes.addFlashAttribute("success",
                    "Payment created successfully! Transaction ID: " + createdPayment.getTransactionId());

            return "redirect:/api/payment/manage";
        } catch (RuntimeException e) {
            logger.error("Error creating payment for bookingId: {}", bookingId, e);
            redirectAttributes.addFlashAttribute("error", "Error creating payment: " + e.getMessage());
            return "redirect:/api/payment/manage";
        }
    }

    /**
     * Update existing payment (Finance Executive only)
     */
    @PostMapping("/update/{id}")
    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
    public String updatePayment(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate inputs
            if (id == null || id <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid payment ID");
                return "redirect:/api/payment/manage";
            }
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/api/payment/manage";
            }
            if (status == null || status.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Status is required");
                return "redirect:/api/payment/manage";
            }

            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error",
                        "Invalid status. Must be COMPLETED, REFUNDED, or FAILED");
                return "redirect:/api/payment/manage";
            }

            // Create PaymentDTO with updated values
            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setAmount(amount);
            paymentDTO.setStatus(status);

            // Update payment
            paymentService.updatePayment(id, paymentDTO);

            logger.info("Payment updated successfully: id={}, amount={}, status={}", id, amount, status);

            redirectAttributes.addFlashAttribute("success", "Payment updated successfully!");

            return "redirect:/api/payment/manage";
        } catch (RuntimeException e) {
            logger.error("Error updating payment: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error updating payment: " + e.getMessage());
            return "redirect:/api/payment/manage";
        }
    }

    /**
     * Delete payment (Finance Executive only)
     */
    @PostMapping("/delete/{paymentId}")
    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
    public String deletePayment(
            @PathVariable Long paymentId,
            RedirectAttributes redirectAttributes) {
        try {
            if (paymentId == null || paymentId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid payment ID");
                return "redirect:/api/payment/manage";
            }

            // Delete payment
            paymentService.deletePayment(paymentId);

            logger.info("Payment deleted successfully: id={}", paymentId);

            redirectAttributes.addFlashAttribute("success",
                    "Payment deleted successfully. Associated booking has been cancelled.");

            return "redirect:/api/payment/manage";
        } catch (RuntimeException e) {
            logger.error("Error deleting payment: id={}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting payment: " + e.getMessage());
            return "redirect:/api/payment/manage";
        }
    }

    /**
     * Refund payment (Finance Executive only)
     */
    @PostMapping("/refund")
    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
    public String refundPayment(
            @RequestParam Long paymentId,
            RedirectAttributes redirectAttributes) {
        try {
            if (paymentId == null || paymentId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid payment ID");
                return "redirect:/api/payment/manage";
            }

            // Refund payment
            paymentService.refundPayment(paymentId);

            logger.info("Payment refunded successfully: id={}", paymentId);

            redirectAttributes.addFlashAttribute("success",
                    "Payment refunded successfully. Associated booking has been cancelled.");

            return "redirect:/api/payment/manage";
        } catch (RuntimeException e) {
            logger.error("Error refunding payment: id={}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Error refunding payment: " + e.getMessage());
            return "redirect:/api/payment/manage";
        }
    }

    /**
     * Show payment page for travelers (authenticated users)
     * FIXED: Combined duplicate method and added BookingService dependency
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/page")
    public String showPaymentPage(@RequestParam Long bookingId, Model model) {
        try {
            if (bookingId == null || bookingId <= 0) {
                logger.warn("Invalid bookingId provided for payment page: {}", bookingId);
                model.addAttribute("error", "Invalid booking ID.");
                return "error";
            }

            // Fetch booking details
            BookingDTO booking = bookingService.getBookingById(bookingId);

            if (booking == null) {
                logger.error("Booking not found: ID={}", bookingId);
                model.addAttribute("error", "Booking not found");
                return "error";
            }

            // Check if payment already exists
            if (booking.getPaymentId() != null) {
                logger.warn("Payment already exists for booking ID: {}", bookingId);
                return "redirect:/api/user/profile?message=Payment already completed";
            }

            // Calculate total with taxes
            Double subtotal = booking.getTotalPrice();
            Double taxes = 45.00; // Fixed tax amount
            Double total = subtotal + taxes;

            // Add attributes to model
            model.addAttribute("booking", booking);
            model.addAttribute("bookingId", bookingId);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("taxes", taxes);
            model.addAttribute("total", total);

            logger.info("Payment page loaded for booking ID: {}", bookingId);
            return "traveler-payment";

        } catch (RuntimeException e) {
            logger.error("Error loading payment page for booking ID: {}", bookingId, e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    /**
     * Process payment for travelers (authenticated users)
     * FIXED: Combined duplicate method with proper validation
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/process")
    public String processPayment(@ModelAttribute PaymentDTO paymentDTO,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        try {
            logger.info("Processing payment for booking ID: {}", paymentDTO.getBookingId());

            // Validate required fields
            if (paymentDTO.getBookingId() == null) {
                throw new RuntimeException("Booking ID is required");
            }
            if (paymentDTO.getAmount() == null || paymentDTO.getAmount() <= 0) {
                throw new RuntimeException("Valid payment amount is required");
            }
            if (paymentDTO.getPaymentMethod() == null || paymentDTO.getPaymentMethod().isEmpty()) {
                throw new RuntimeException("Payment method is required");
            }

            // Method-specific validation
            if ("CARD".equalsIgnoreCase(paymentDTO.getPaymentMethod())) {
                if (paymentDTO.getCardNumber() == null || paymentDTO.getCardNumber().trim().isEmpty()) {
                    throw new RuntimeException("Card number is required");
                }
                if (paymentDTO.getCardName() == null || paymentDTO.getCardName().trim().isEmpty()) {
                    throw new RuntimeException("Cardholder name is required");
                }
                if (paymentDTO.getExpiry() == null || paymentDTO.getExpiry().trim().isEmpty()) {
                    throw new RuntimeException("Card expiry date is required");
                }
                if (paymentDTO.getCvv() == null || paymentDTO.getCvv().trim().isEmpty()) {
                    throw new RuntimeException("CVV is required");
                }
                if (paymentDTO.getEmail() == null || paymentDTO.getEmail().trim().isEmpty()) {
                    throw new RuntimeException("Email is required");
                }
            } else if ("EZ_CASH".equalsIgnoreCase(paymentDTO.getPaymentMethod())) {
                if (paymentDTO.getMobile() == null || paymentDTO.getMobile().trim().isEmpty()) {
                    throw new RuntimeException("Mobile number is required");
                }
                // Validate mobile number format
                if (!paymentDTO.getMobile().matches("^7\\d{8}$")) {
                    throw new RuntimeException("Invalid mobile number format. Use format: 7XXXXXXXX");
                }
            }

            // Process payment
            PaymentDTO processedPayment = paymentService.processPayment(paymentDTO);

            logger.info("Payment processed successfully: ID={}, TxnID={}",
                    processedPayment.getId(), processedPayment.getTransactionId());

            // Redirect to confirmation page with success message
            redirectAttributes.addFlashAttribute("success",
                    "Payment completed successfully! Transaction ID: " + processedPayment.getTransactionId());
            redirectAttributes.addFlashAttribute("transactionId", processedPayment.getTransactionId());
            redirectAttributes.addFlashAttribute("bookingId", paymentDTO.getBookingId());
            redirectAttributes.addFlashAttribute("amount", processedPayment.getAmount());
            
            // Add booking details for confirmation page
            try {
                BookingDTO booking = bookingService.getBookingById(paymentDTO.getBookingId());
                redirectAttributes.addFlashAttribute("booking", booking);
            } catch (Exception e) {
                logger.warn("Could not fetch booking details for confirmation page: {}", e.getMessage());
            }

            return "redirect:/api/payment/confirmation";

        } catch (RuntimeException e) {
            logger.error("Payment processing failed for booking ID: {}", paymentDTO.getBookingId(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/api/payment/page?bookingId=" + paymentDTO.getBookingId();
        }
    }

    /**
     * Payment confirmation page
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/confirmation")
    public String showConfirmation(Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            // Get booking from flash attributes
            BookingDTO booking = (BookingDTO) model.asMap().get("booking");
            if (booking != null) {
                model.addAttribute("booking", booking);
            }
            // Success and booking details are passed via flash attributes from processPayment
            return "booking-confirmation";
        } catch (Exception e) {
            logger.error("Error loading confirmation page", e);
            model.addAttribute("error", "Unable to load confirmation page");
            return "error";
        }
    }
}
