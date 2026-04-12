package com.skylinkapplication.skylinkairlinereservationsystem.controller;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.*;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    @Autowired
    private FlightService flightService;
    @Autowired
    private PromotionService promotionService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private FeedbackService feedbackService;
    @Autowired
    private SupportService supportService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
    public String adminDashboard() {
        return "admin-dashboard";
    }
    @GetMapping("/user-management")
    @PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
    public String userManagementDashboard() {
        return "user-dashboard";
    }
    @GetMapping("/promotion-management")
    @PreAuthorize("hasRole('MARKETING_EXECUTIVE')")
    public String promotionManagementDashboard() {
        return "promotion-manager-dashboard";
    }
    @GetMapping("/reservation-manager")
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    public String reservationManagerDashboard(Model model) {
        try {
            List<BookingDTO> allBookings = bookingService.getAllBookings();
            model.addAttribute("bookings", allBookings);
            
            // Add flights for the flights tab
            List<FlightDTO> allFlights = flightService.getAllFlights();
            model.addAttribute("allFlights", allFlights);
            
            // Add payments for the payments tab
            List<PaymentDTO> allPayments = paymentService.getAllPayments();
            model.addAttribute("allPayments", allPayments);
            
            logger.info("Reservation manager dashboard loaded successfully with {} bookings, {} flights, and {} payments", 
                allBookings != null ? allBookings.size() : 0,
                allFlights != null ? allFlights.size() : 0,
                allPayments != null ? allPayments.size() : 0);
            
            // Log payment details for debugging
            if (allPayments != null) {
                logger.debug("Payment details:");
                for (PaymentDTO payment : allPayments) {
                    logger.debug("  Payment ID: {}, Booking ID: {}, Amount: {}, Status: {}", 
                        payment.getId(), payment.getBookingId(), payment.getAmount(), payment.getStatus());
                }
            }
            
            return "booking-management-dashboard";
        } catch (Exception e) {
            logger.error("Error retrieving data for reservation manager dashboard", e);
            model.addAttribute("error", "Unable to load dashboard data. Please try again.");
            // Add empty collections to prevent template errors
            model.addAttribute("bookings", Collections.emptyList());
            model.addAttribute("allFlights", Collections.emptyList());
            model.addAttribute("allPayments", Collections.emptyList());
            return "booking-management-dashboard";
        }
    }
    
    // Debug endpoint to check payments
    @GetMapping("/reservation-manager/debug-payments")
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @ResponseBody
    public String debugPayments() {
        try {
            List<PaymentDTO> allPayments = paymentService.getAllPayments();
            StringBuilder result = new StringBuilder();
            result.append("<h2>Debug Payments</h2>");
            result.append("<p>Total payments: ").append(allPayments != null ? allPayments.size() : 0).append("</p>");
            
            if (allPayments != null && !allPayments.isEmpty()) {
                result.append("<table border='1'><tr><th>ID</th><th>Booking ID</th><th>Amount</th><th>Status</th></tr>");
                for (PaymentDTO payment : allPayments) {
                    result.append("<tr>")
                          .append("<td>").append(payment.getId()).append("</td>")
                          .append("<td>").append(payment.getBookingId()).append("</td>")
                          .append("<td>").append(payment.getAmount()).append("</td>")
                          .append("<td>").append(payment.getStatus()).append("</td>")
                          .append("</tr>");
                }
                result.append("</table>");
            } else {
                result.append("<p>No payments found</p>");
            }
            
            return result.toString();
        } catch (Exception e) {
            logger.error("Error in debugPayments", e);
            return "<p>Error: " + e.getMessage() + "</p>";
        }
    }
    
    // Debug endpoint to check payments directly from repository
    @GetMapping("/reservation-manager/debug-payments-db")
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @ResponseBody
    public String debugPaymentsFromDB() {
        try {
            List<Payment> allPayments = paymentRepository.findAll();
            StringBuilder result = new StringBuilder();
            result.append("<h2>Debug Payments from Database</h2>");
            result.append("<p>Total payments in DB: ").append(allPayments != null ? allPayments.size() : 0).append("</p>");
            
            if (allPayments != null && !allPayments.isEmpty()) {
                result.append("<table border='1'><tr><th>ID</th><th>Booking ID</th><th>Amount</th><th>Status</th></tr>");
                for (Payment payment : allPayments) {
                    result.append("<tr>")
                          .append("<td>").append(payment.getId()).append("</td>")
                          .append("<td>").append(payment.getBooking() != null ? payment.getBooking().getId() : "null").append("</td>")
                          .append("<td>").append(payment.getAmount()).append("</td>")
                          .append("<td>").append(payment.getStatus()).append("</td>")
                          .append("</tr>");
                }
                result.append("</table>");
            } else {
                result.append("<p>No payments found in database</p>");
            }
            
            return result.toString();
        } catch (Exception e) {
            logger.error("Error in debugPaymentsFromDB", e);
            return "<p>Error: " + e.getMessage() + "</p>";
        }
    }
    
    // Simple test endpoint to check raw data
    @GetMapping("/reservation-manager/test-data")
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @ResponseBody
    public Map<String, Object> testData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<BookingDTO> bookings = bookingService.getAllBookings();
            List<FlightDTO> flights = flightService.getAllFlights();
            List<PaymentDTO> payments = paymentService.getAllPayments();
            
            result.put("bookingsCount", bookings != null ? bookings.size() : 0);
            result.put("flightsCount", flights != null ? flights.size() : 0);
            result.put("paymentsCount", payments != null ? payments.size() : 0);
            
            // Add first few payments for inspection
            if (payments != null && !payments.isEmpty()) {
                result.put("samplePayments", payments.stream().limit(5).collect(Collectors.toList()));
            }
            
            result.put("status", "success");
        } catch (Exception e) {
            logger.error("Error in testData", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        
        return result;
    }
//    @GetMapping("/finance")
//    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
//    public String financeDashboard() {
//        return "finance-dashboard";
//    }


    @GetMapping("/finance")
    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
    public String financeDashboard(Model model) {
        try {
            logger.info("Finance Executive accessing finance dashboard");

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

            logger.info("Finance dashboard loaded successfully with {} payments", totalPayments);

            return "finance-dashboard";
        } catch (Exception e) {
            logger.error("Error loading finance dashboard", e);
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


    @GetMapping("/flight-admin")
    @PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
    public String flightAdminDashboard(
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String routeFilter,
            Model model) {
        try {
            List<FlightDTO> flights;
            if (dateFilter != null || statusFilter != null || routeFilter != null) {
                Date parsedDate = null;
                if (dateFilter != null && !dateFilter.isEmpty()) {
                    parsedDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateFilter);
                }
                // Extract origin and destination from routeFilter
                String origin = null;
                String destination = null;
                if (routeFilter != null && routeFilter.contains("-")) {
                    String[] parts = routeFilter.split("-");
                    if (parts.length >= 2) {
                        origin = parts[0];
                        destination = parts[1];
                    }
                }
                // Use the correct method name with all parameters
                flights = flightService.searchFlightsWithOrigin(parsedDate, origin, destination, null, statusFilter);
            } else {
                flights = flightService.getAllFlights();
            }
            model.addAttribute("flights", flights);
            model.addAttribute("dateFilter", dateFilter);
            model.addAttribute("statusFilter", statusFilter);
            model.addAttribute("routeFilter", routeFilter);
            logger.info("Flight schedule retrieved: {} flights found", flights.size());
            return "flight-admin-schedule-management";
        } catch (Exception e) {
            logger.error("Error retrieving flight schedule", e);
            model.addAttribute("error", "Unable to load flight schedule: " + e.getMessage());
            model.addAttribute("flights", Collections.emptyList());
            return "flight-admin-schedule-management";
        }
    }
    @GetMapping("/marketing-manager")
    @PreAuthorize("hasRole('MARKETING_EXECUTIVE')")
    public String marketingManagerDashboard(Model model) {
        try {
            List<PromotionDTO> promotions = promotionService.getAllPromotions();
            model.addAttribute("promotions", promotions);
            logger.info("Marketing manager dashboard accessed with {} promotions", promotions.size());
            return "promotion-manager-dashboard";
        } catch (Exception e) {
            logger.error("Error loading promotions for marketing manager dashboard", e);
            model.addAttribute("error", "Unable to load promotions: " + e.getMessage());
            model.addAttribute("promotions", Collections.emptyList());
            return "promotion-manager-dashboard";
        }
    }
    @GetMapping("/customer-support")
    @PreAuthorize("hasRole('CUSTOMER_SUPPORT_OFFICER')")
    public String customerSupportDashboard(Model model) {
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

            logger.info("Customer support dashboard accessed successfully");
            return "customer-support-manage-dashboard";
        } catch (Exception e) {
            logger.error("Error loading customer support dashboard", e);
            model.addAttribute("error", "Unable to load dashboard. Please try again.");
            return "customer-support-manage-dashboard";
        }
    }
    @PostMapping("/flight-admin")
    @PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
    public String createFlight(
            @RequestParam String flightNumber,
            @RequestParam(required = false) String aircraftType,
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String departureDate,
            @RequestParam String arrivalDate,
            @RequestParam Double price,
            @RequestParam String cabinClass,
            @RequestParam Integer seatsAvailable,
            Model model) {
        try {
            // Validate inputs
            if (flightNumber == null || flightNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Flight number cannot be empty.");
            }
            if (origin == null || origin.trim().isEmpty()) {
                throw new IllegalArgumentException("Origin cannot be empty.");
            }
            if (destination == null || destination.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination cannot be empty.");
            }
            if (cabinClass == null || cabinClass.trim().isEmpty()) {
                throw new IllegalArgumentException("Cabin class cannot be empty.");
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            Date parsedDepartureDate;
            Date parsedArrivalDate;
            try {
                parsedDepartureDate = dateFormat.parse(departureDate);
                parsedArrivalDate = dateFormat.parse(arrivalDate);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format.");
            }
            if (parsedDepartureDate.after(parsedArrivalDate)) {
                throw new IllegalArgumentException("Departure date must be before arrival date.");
            }
            if (price == null || price <= 0) {
                throw new IllegalArgumentException("Price must be positive.");
            }
            if (seatsAvailable == null || seatsAvailable <= 0) {
                throw new IllegalArgumentException("Seats available must be positive.");
            }
            // Create FlightDTO
            FlightDTO flightDTO = new FlightDTO();
            flightDTO.setFlightNumber(flightNumber);
            flightDTO.setAircraftType(aircraftType != null ? aircraftType : "");
            flightDTO.setOrigin(origin);
            flightDTO.setDestination(destination);
            flightDTO.setDepartureDate(parsedDepartureDate);
            flightDTO.setArrivalDate(parsedArrivalDate);
            flightDTO.setPrice(price);
            flightDTO.setCabinClass(cabinClass);
            flightDTO.setSeatsAvailable(seatsAvailable);
            flightDTO.setStatus("Scheduled");
            // Call service to create flight
            flightService.createFlight(flightDTO);
            logger.info("Flight created successfully: flightNumber={}", flightNumber);
            model.addAttribute("success", "Flight created successfully.");
            // Refresh schedule
            model.addAttribute("flights", flightService.getAllFlights());
            return "flight-admin-schedule-management";
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating flight: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("flights", flightService.getAllFlights());
            return "flight-admin-schedule-management";
        } catch (Exception e) {
            logger.error("Error creating flight: flightNumber={}, origin={}, destination={}",
                    flightNumber, origin, destination, e);
            model.addAttribute("error", "Unable to create flight: " + e.getMessage());
            model.addAttribute("flights", flightService.getAllFlights());
            return "flight-admin-schedule-management";
        }
    }
    @PostMapping("/flight-admin/update/{id}")
    @PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
    public String updateFlight(
            @PathVariable Long id,
            @RequestParam String flightNumber,
            @RequestParam(required = false) String aircraftType,
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam String departureDate,
            @RequestParam String arrivalDate,
            @RequestParam Double price,
            @RequestParam String cabinClass,
            @RequestParam Integer seatsAvailable,
            @RequestParam String status,
            Model model) {
        try {
            // Validate inputs
            if (id == null) {
                throw new IllegalArgumentException("Flight ID cannot be null.");
            }
            if (flightNumber == null || flightNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Flight number cannot be empty.");
            }
            if (origin == null || origin.trim().isEmpty()) {
                throw new IllegalArgumentException("Origin cannot be empty.");
            }
            if (destination == null || destination.trim().isEmpty()) {
                throw new IllegalArgumentException("Destination cannot be empty.");
            }
            if (cabinClass == null || cabinClass.trim().isEmpty()) {
                throw new IllegalArgumentException("Cabin class cannot be empty.");
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            Date parsedDepartureDate;
            Date parsedArrivalDate;
            try {
                parsedDepartureDate = dateFormat.parse(departureDate);
                parsedArrivalDate = dateFormat.parse(arrivalDate);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format.");
            }
            if (parsedDepartureDate.after(parsedArrivalDate)) {
                throw new IllegalArgumentException("Departure date must be before arrival date.");
            }
            if (price == null || price <= 0) {
                throw new IllegalArgumentException("Price must be positive.");
            }
            if (seatsAvailable == null || seatsAvailable < 0) {
                throw new IllegalArgumentException("Seats available cannot be negative.");
            }
            // Create FlightDTO
            FlightDTO flightDTO = new FlightDTO();
            flightDTO.setFlightNumber(flightNumber);
            flightDTO.setAircraftType(aircraftType != null ? aircraftType : "");
            flightDTO.setOrigin(origin);
            flightDTO.setDestination(destination);
            flightDTO.setDepartureDate(parsedDepartureDate);
            flightDTO.setArrivalDate(parsedArrivalDate);
            flightDTO.setPrice(price);
            flightDTO.setCabinClass(cabinClass);
            flightDTO.setSeatsAvailable(seatsAvailable);
            flightDTO.setStatus(status != null && !status.isEmpty() ? status : "Scheduled");
            // Call service to update flight
            flightService.updateFlight(id, flightDTO);
            logger.info("Flight updated successfully: id={}", id);
            model.addAttribute("success", "Flight updated successfully.");
            // Refresh schedule
            model.addAttribute("flights", flightService.getAllFlights());
            return "flight-admin-schedule-management";
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating flight: id={}, error={}", id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("flights", flightService.getAllFlights());
            return "flight-admin-schedule-management";
        } catch (Exception e) {
            logger.error("Error updating flight: id={}", id, e);
            model.addAttribute("error", "Unable to update flight: " + e.getMessage());
            model.addAttribute("flights", flightService.getAllFlights());
            return "flight-admin-schedule-management";
        }
    }
    @PostMapping("/flight-admin/delete/{id}")
    @PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
    public String deleteFlight(@PathVariable Long id, Model model) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Flight ID cannot be null.");
            }
            flightService.deleteFlight(id);
            logger.info("Flight deleted successfully: id={}", id);
            model.addAttribute("success", "Flight deleted successfully.");
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error deleting flight: id={}, error={}", id, e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting flight: id={}", id, e);
            model.addAttribute("error", "Unable to delete flight: " + e.getMessage());
        }
        // Refresh schedule
        model.addAttribute("flights", flightService.getAllFlights());
        return "flight-admin-schedule-management";
    }
}