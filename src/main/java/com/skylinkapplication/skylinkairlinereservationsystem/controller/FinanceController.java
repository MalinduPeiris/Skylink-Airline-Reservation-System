package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/dashboard/finance")
public class FinanceController {
    private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    /**
     * All Payments Tab - Show all payments with CRUD
     */
    @GetMapping("/all-payments")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String showAllPayments(Model model) {
        try {
            List<PaymentDTO> allPayments = paymentService.getAllPayments();
            model.addAttribute("payments", allPayments);
            model.addAttribute("activeTab", "all");

            logger.info("Loaded {} payments in all-payments tab", allPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading all payments", e);
            model.addAttribute("error", "Unable to load payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "all");
            return "finance-payments-tab";
        }
    }

    /**
     * Completed Payments Tab
     */
    @GetMapping("/completed")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String showCompletedPayments(Model model) {
        try {
            List<PaymentDTO> completedPayments = paymentService.getPaymentsByStatus("COMPLETED");
            model.addAttribute("payments", completedPayments);
            model.addAttribute("activeTab", "completed");

            logger.info("Loaded {} completed payments", completedPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading completed payments", e);
            model.addAttribute("error", "Unable to load completed payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "completed");
            return "finance-payments-tab";
        }
    }

    /**
     * Failed Payments Tab
     */
    @GetMapping("/failed")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String showFailedPayments(Model model) {
        try {
            List<PaymentDTO> failedPayments = paymentService.getPaymentsByStatus("FAILED");
            model.addAttribute("payments", failedPayments);
            model.addAttribute("activeTab", "failed");

            logger.info("Loaded {} failed payments", failedPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading failed payments", e);
            model.addAttribute("error", "Unable to load failed payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "failed");
            return "finance-payments-tab";
        }
    }

    /**
     * Refunded Payments Tab
     */
    @GetMapping("/refunded")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String showRefundedPayments(Model model) {
        try {
            List<PaymentDTO> refundedPayments = paymentService.getPaymentsByStatus("REFUNDED");
            model.addAttribute("payments", refundedPayments);
            model.addAttribute("activeTab", "refunded");

            logger.info("Loaded {} refunded payments", refundedPayments.size());
            return "finance-payments-tab";
        } catch (Exception e) {
            logger.error("Error loading refunded payments", e);
            model.addAttribute("error", "Unable to load refunded payments: " + e.getMessage());
            model.addAttribute("payments", List.of());
            model.addAttribute("activeTab", "refunded");
            return "finance-payments-tab";
        }
    }

    /**
     * Cancelled Bookings Tab - Shows bookings with CANCELLED status
     */
    @GetMapping("/cancelled-bookings")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String showCancelledBookings(Model model) {
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

            logger.info("Loaded {} cancelled bookings", cancelledBookings.size());
            return "finance-cancelled-bookings";
        } catch (Exception e) {
            logger.error("Error loading cancelled bookings", e);
            model.addAttribute("error", "Unable to load cancelled bookings: " + e.getMessage());
            model.addAttribute("bookings", List.of());
            model.addAttribute("activeTab", "cancelled");
            return "finance-cancelled-bookings";
        }
    }

    /**
     * Create Payment
     */
    @PostMapping("/payment/create")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String createPayment(
            @RequestParam Long bookingId,
            @RequestParam Double amount,
            @RequestParam String status,
            @RequestParam String activeTab,
            RedirectAttributes redirectAttributes) {
        try {
            if (bookingId == null || bookingId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Invalid booking ID");
                return "redirect:/dashboard/finance/" + activeTab;
            }
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/dashboard/finance/" + activeTab;
            }

            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid status");
                return "redirect:/dashboard/finance/" + activeTab;
            }

            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setBookingId(bookingId);
            paymentDTO.setAmount(amount);
            paymentDTO.setStatus(status);

            PaymentDTO createdPayment = paymentService.createPayment(paymentDTO);

            logger.info("Payment created successfully: id={}", createdPayment.getId());
            redirectAttributes.addFlashAttribute("success",
                    "Payment #" + createdPayment.getId() + " created successfully!");

            return "redirect:/dashboard/finance/" + activeTab;
        } catch (RuntimeException e) {
            logger.error("Error creating payment", e);
            redirectAttributes.addFlashAttribute("error", "Error creating payment: " + e.getMessage());
            return "redirect:/dashboard/finance/" + activeTab;
        }
    }

    /**
     * Update Payment - FIXED VERSION
     */
    @PostMapping("/payment/update/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String updatePayment(
            @PathVariable Long id,
            @RequestParam Double amount,
            @RequestParam String status,
            @RequestParam String transactionId,
            @RequestParam String activeTab,
            RedirectAttributes redirectAttributes) {
        try {
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Amount must be greater than zero");
                return "redirect:/dashboard/finance/" + activeTab;
            }

            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid status");
                return "redirect:/dashboard/finance/" + activeTab;
            }

            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setAmount(amount);
            paymentDTO.setStatus(status);
            paymentDTO.setTransactionId(transactionId);

            paymentService.updatePayment(id, paymentDTO);

            logger.info("Payment updated successfully: id={}", id);
            redirectAttributes.addFlashAttribute("success", "Payment #" + id + " updated successfully!");

            return "redirect:/dashboard/finance/" + activeTab;
        } catch (RuntimeException e) {
            logger.error("Error updating payment: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error updating payment: " + e.getMessage());
            return "redirect:/dashboard/finance/" + activeTab;
        }
    }

    /**
     * Delete Payment
     */
    @PostMapping("/payment/delete/{id}")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String deletePayment(
            @PathVariable Long id,
            @RequestParam String activeTab,
            RedirectAttributes redirectAttributes) {
        try {
            paymentService.deletePayment(id);

            logger.info("Payment deleted successfully: id={}", id);
            redirectAttributes.addFlashAttribute("success", "Payment #" + id + " deleted successfully!");

            return "redirect:/dashboard/finance/" + activeTab;
        } catch (RuntimeException e) {
            logger.error("Error deleting payment: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting payment: " + e.getMessage());
            return "redirect:/dashboard/finance/" + activeTab;
        }
    }

    /**
     * Update Payment Status from Cancelled Bookings - NEW
     */
    @PostMapping("/booking/update-payment-status/{paymentId}")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String updatePaymentStatus(
            @PathVariable Long paymentId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            // Validate status enum
            try {
                Payment.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Invalid payment status");
                return "redirect:/dashboard/finance/cancelled-bookings";
            }

            PaymentDTO paymentDTO = new PaymentDTO();
            paymentDTO.setStatus(status);

            paymentService.updatePayment(paymentId, paymentDTO);

            logger.info("Payment status updated successfully: id={}, new status={}", paymentId, status);
            redirectAttributes.addFlashAttribute("success",
                    "Payment #" + paymentId + " status updated to " + status + " successfully!");

            return "redirect:/dashboard/finance/cancelled-bookings";
        } catch (RuntimeException e) {
            logger.error("Error updating payment status: id={}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Error updating payment status: " + e.getMessage());
            return "redirect:/dashboard/finance/cancelled-bookings";
        }
    }

    /**
     * Refund Payment from Cancelled Bookings
     */
    @PostMapping("/booking/refund-payment/{paymentId}")
    @PreAuthorize("hasAnyRole('FINANCE_EXECUTIVE', 'IT_SYSTEM_ENGINEER')")
    public String refundPaymentFromBooking(
            @PathVariable Long paymentId,
            RedirectAttributes redirectAttributes) {
        try {
            paymentService.refundPayment(paymentId);

            logger.info("Payment refunded successfully: id={}", paymentId);
            redirectAttributes.addFlashAttribute("success",
                    "Payment #" + paymentId + " refunded successfully!");

            return "redirect:/dashboard/finance/cancelled-bookings";
        } catch (RuntimeException e) {
            logger.error("Error refunding payment: id={}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Error refunding payment: " + e.getMessage());
            return "redirect:/dashboard/finance/cancelled-bookings";
        }
    }
}