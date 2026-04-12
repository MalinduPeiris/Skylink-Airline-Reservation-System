package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingRequestDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.FlightDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PassengerDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FlightService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.ConfigurationManager;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/booking")
public class BookingController {
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FlightService flightService;
    
    @Autowired
    private PaymentService paymentService;

    /**
     * Display booking management dashboard for RESERVATION_MANAGER
     */
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @GetMapping("/dashboard")
    public String viewBookings(Model model) {
        try {
            List<BookingDTO> bookings = bookingService.getAllBookings();
            model.addAttribute("bookings", bookings);

            // Add flights and users for dropdowns
            model.addAttribute("flights", flightRepository.findAll());
            model.addAttribute("users", userRepository.findAll());
            
            // Add all flights for the flights tab
            List<FlightDTO> allFlights = flightService.getAllFlights();
            model.addAttribute("allFlights", allFlights);
            
            // Add all payments for the payments tab
            List<PaymentDTO> allPayments = paymentService.getAllPayments();
            model.addAttribute("allPayments", allPayments);

            logger.info("Booking dashboard loaded with {} bookings, {} flights, and {} payments", 
                       bookings.size(), allFlights.size(), allPayments.size());
            return "booking-management-dashboard";
        } catch (RuntimeException e) {
            logger.error("Error retrieving bookings", e);
            model.addAttribute("error", "Unable to load bookings. Please try again.");
            return "booking-management-dashboard";
        }
    }

    /**
     * Create a new booking via Thymeleaf form (RESERVATION_MANAGER only)
     */
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @PostMapping("/create-booking")
    public String createBooking(@RequestParam Long flightId,
                                @RequestParam Long userId,
                                @RequestParam Integer passengers,
                                @RequestParam(required = false) Double bookingExtras,
                                @RequestParam(required = false) String promoCode,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            // Validate inputs
            if (flightId == null || flightId <= 0) {
                throw new IllegalArgumentException("Invalid flight selected");
            }
            if (userId == null || userId <= 0) {
                throw new IllegalArgumentException("Invalid user selected");
            }
            if (passengers == null || passengers < 1 || passengers > 5) {
                throw new IllegalArgumentException("Invalid number of passengers (1-5)");
            }

            // Fetch flight and user
            Flight flight = flightRepository.findById(flightId)
                    .orElseThrow(() -> new IllegalArgumentException("Flight not found"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Check seat availability
            if (flight.getSeatsAvailable() < passengers) {
                throw new IllegalArgumentException("Not enough seats available on this flight");
            }

            // Create booking DTO
            BookingDTO bookingDTO = new BookingDTO();
            bookingDTO.setFlightId(flightId);
            bookingDTO.setUserId(userId);
            bookingDTO.setPassengers(passengers);
            bookingDTO.setBookingExtras(bookingExtras != null ? bookingExtras : 0.0);
            bookingDTO.setPromoCode(promoCode);
            bookingDTO.setStatus(status);
            bookingDTO.setUser(user);
            bookingDTO.setFlight(flight);

            // Create booking
            BookingDTO createdBooking = bookingService.createBooking(bookingDTO);

            logger.info("Booking created successfully by RESERVATION_MANAGER: {}", createdBooking.getId());
            redirectAttributes.addFlashAttribute("success",
                    String.format("Booking BK-%d created successfully for %s", createdBooking.getId(), user.getUsername()));

            return "redirect:/api/booking/dashboard";
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error creating booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/api/booking/dashboard";
        } catch (Exception e) {
            logger.error("Error creating booking", e);
            redirectAttributes.addFlashAttribute("error", "Error creating booking: " + e.getMessage());
            return "redirect:/api/booking/dashboard";
        }
    }

    /**
     * Show booking page for authenticated users (travelers)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/page")
    public String showBookingPage(@RequestParam Long flightId,
                                  @RequestParam(defaultValue = "1") Integer passengers,
                                  @RequestParam(required = false) String destination,
                                  Model model,
                                  Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            Flight flight = flightRepository.findById(flightId)
                    .orElseThrow(() -> new RuntimeException("Flight not found with ID: " + flightId));

            // Check seat availability
            if (flight.getSeatsAvailable() < passengers) {
                model.addAttribute("error", "Not enough seats available on this flight");
                return "redirect:/api/flight/all";
            }

            // Get the destination city (not country)
            String destinationCity = null;
            if (destination != null && !destination.isEmpty()) {
                destinationCity = destination;
            } else if (flight.getDestination() != null) {
                destinationCity = flight.getDestination();
            }

            BookingRequestDTO bookingRequest = new BookingRequestDTO();
            bookingRequest.setFlightId(flight.getId());
            bookingRequest.setUserId(user.getId());
            bookingRequest.setPassengerCount(passengers);

            List<PassengerDTO> passengerList = new ArrayList<>();
            for (int i = 0; i < passengers; i++) {
                PassengerDTO passenger = new PassengerDTO();
                if (i == 0) {
                    passenger.setEmail(user.getEmail());
                }
                // Set the destination city for all passengers
                if (destinationCity != null) {
                    passenger.setCountry(destinationCity);
                }
                passengerList.add(passenger);
            }
            bookingRequest.setPassengers(passengerList);

            model.addAttribute("bookingRequest", bookingRequest);
            model.addAttribute("flight", flight);
            model.addAttribute("passengerCount", passengers);
            model.addAttribute("userEmail", user.getEmail());
            model.addAttribute("destinationCity", destinationCity);

            logger.info("Booking page loaded for user: {} and flight: {}", username, flight.getFlightNumber());
            return "booking-page";
        } catch (RuntimeException e) {
            logger.error("Error loading booking page", e);
            ConfigurationManager config = ConfigurationManager.getInstance();
            if (config.isMaintenanceMode()) {
                model.addAttribute("error", "System is under maintenance");
                return "error";
            }
            model.addAttribute("error", e.getMessage());
            return "redirect:/api/flight/all";
        }
    }

    // Helper method to create destination to country mapping
    private java.util.Map<String, String> createDestinationToCountryMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("Colombo", "Sri Lanka");
        map.put("London", "United Kingdom");
        map.put("Los Angeles", "United States");
        map.put("Sydney", "Australia");
        map.put("Dubai", "United Arab Emirates");
        map.put("Paris", "France");
        map.put("New York", "United States");
        map.put("Tokyo", "Japan");
        map.put("Singapore", "Singapore");
        map.put("Bangkok", "Thailand");
        map.put("Kuala Lumpur", "Malaysia");
        map.put("Hong Kong", "Hong Kong");
        map.put("Seoul", "South Korea");
        map.put("Beijing", "China");
        map.put("Shanghai", "China");
        map.put("Delhi", "India");
        map.put("Mumbai", "India");
        map.put("Frankfurt", "Germany");
        map.put("Amsterdam", "Netherlands");
        map.put("Rome", "Italy");
        map.put("Madrid", "Spain");
        map.put("Barcelona", "Spain");
        map.put("Vienna", "Austria");
        map.put("Zurich", "Switzerland");
        map.put("Geneva", "Switzerland");
        map.put("Stockholm", "Sweden");
        map.put("Copenhagen", "Denmark");
        map.put("Oslo", "Norway");
        map.put("Helsinki", "Finland");
        map.put("Moscow", "Russia");
        map.put("Istanbul", "Turkey");
        return map;
    }

    /**
     * Create booking with passengers (from booking-page.html form)
     * FIXED: Only ONE method with this mapping now
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create-with-passengers")
    public String createBookingWithPassengers(@ModelAttribute BookingRequestDTO bookingRequest,
                                              RedirectAttributes redirectAttributes) {
        try {
            // Debug logging
            logger.info("=== BOOKING REQUEST RECEIVED ===");

            logger.info("BookingRequest: {}", bookingRequest);
            logger.info("FlightId: {}", bookingRequest.getFlightId());
            logger.info("PassengerCount: {}", bookingRequest.getPassengerCount());
            logger.info("Passengers: {}", bookingRequest.getPassengers() != null ? bookingRequest.getPassengers().size() : "null");
            logger.info("BookingExtras: {}", bookingRequest.getBookingExtras());
            logger.info("PromoCode: {}", bookingRequest.getPromoCode());
            logger.info("TotalPrice: {}", bookingRequest.getTotalPrice());

            // Get authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            logger.info("Authenticated user: {}", username);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // Set user ID (override any client-side value for security)
            bookingRequest.setUserId(user.getId());
            logger.info("User ID set to: {}", user.getId());

            // Validate booking request
            if (bookingRequest.getFlightId() == null) {
                throw new RuntimeException("Flight ID is required");
            }
            if (bookingRequest.getPassengerCount() == null || bookingRequest.getPassengerCount() <= 0) {
                throw new RuntimeException("Valid passenger count is required (got: " + bookingRequest.getPassengerCount() + ")");
            }
            if (bookingRequest.getPassengers() == null || bookingRequest.getPassengers().isEmpty()) {
                throw new RuntimeException("Passenger details are required (got: " +
                        (bookingRequest.getPassengers() != null ? bookingRequest.getPassengers().size() : "null") + ")");
            }
            if (bookingRequest.getPassengers().size() != bookingRequest.getPassengerCount()) {
                throw new RuntimeException("Passenger count mismatch. Expected: " + bookingRequest.getPassengerCount() +
                        ", Got: " + bookingRequest.getPassengers().size());
            }

            // Log passenger details
            for (int i = 0; i < bookingRequest.getPassengers().size(); i++) {
                PassengerDTO p = bookingRequest.getPassengers().get(i);
                logger.info("Passenger {}: {} {} - {}", i, p.getFirstName(), p.getLastName(), p.getEmail());
            }

            logger.info("Creating booking for user: {}, flight: {}, passengers: {}",
                    username, bookingRequest.getFlightId(), bookingRequest.getPassengerCount());

            // Create booking with passengers
            BookingDTO createdBooking = bookingService.createBookingWithPassengers(bookingRequest);

            logger.info("Booking created successfully: ID={}, User={}, Total=${}",
                    createdBooking.getId(), username, createdBooking.getTotalPrice());

            // Add success message
            redirectAttributes.addFlashAttribute("success",
                    "Booking created successfully! Booking ID: BK-" + createdBooking.getId());

            // Redirect to payment page
            return "redirect:/api/payment/page?bookingId=" + createdBooking.getId();

        } catch (RuntimeException e) {
            logger.error("Error creating booking with passengers: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Booking creation failed: " + e.getMessage());

            // Redirect back to booking page with error
            if (bookingRequest.getFlightId() != null) {
                return "redirect:/api/booking/page?flightId=" + bookingRequest.getFlightId() +
                        "&passengers=" + (bookingRequest.getPassengerCount() != null ? bookingRequest.getPassengerCount() : 1);
            }
            return "redirect:/api/flight/all";
        }
    }

    /**
     * Update booking via Thymeleaf form (RESERVATION_MANAGER only)
     */
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @PostMapping("/update/{id}")
    public String updateBooking(@PathVariable Long id,
                                @RequestParam Integer passengers,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("Invalid booking ID");
            }
            if (passengers == null || passengers < 1 || passengers > 5) {
                throw new IllegalArgumentException("Invalid number of passengers (1-5)");
            }

            // Create update DTO
            BookingDTO updateDTO = new BookingDTO();
            updateDTO.setPassengers(passengers);
            updateDTO.setStatus(status);

            BookingDTO updatedBooking = bookingService.updateBooking(id, updateDTO);

            logger.info("Booking updated successfully: BK-{}", id);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Booking BK-%d updated successfully", id));

            return "redirect:/api/booking/dashboard";
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error updating booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/api/booking/dashboard";
        } catch (Exception e) {
            logger.error("Error updating booking: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error updating booking: " + e.getMessage());
            return "redirect:/api/booking/dashboard";
        }
    }

    /**
     * Delete booking via Thymeleaf form (RESERVATION_MANAGER only)
     */
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @PostMapping("/delete/{id}")
    public String deleteBooking(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("Invalid booking ID");
            }

            bookingService.deleteBooking(id);

            logger.info("Booking deleted successfully: BK-{}", id);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Booking BK-%d deleted successfully", id));

            return "redirect:/api/booking/dashboard";
        } catch (IllegalArgumentException e) {
            logger.warn("Validation error deleting booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/api/booking/dashboard";
        } catch (Exception e) {
            logger.error("Error deleting booking: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting booking: " + e.getMessage());
            return "redirect:/api/booking/dashboard";
        }
    }

    /**
     * Cancel booking (for authenticated users)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/cancel")
    public String cancelBooking(@RequestParam Long bookingId,
                                @RequestParam Long userId,
                                RedirectAttributes redirectAttributes) {
        try {
            if (bookingId == null || bookingId <= 0) {
                logger.warn("Invalid bookingId provided for cancellation: {}", bookingId);
                redirectAttributes.addFlashAttribute("error", "Invalid booking ID.");
                return "redirect:/api/user/profile";
            }

            bookingService.cancelBooking(bookingId);
            logger.info("Booking cancelled successfully: {}", bookingId);
            redirectAttributes.addFlashAttribute("success", "Booking cancelled successfully.");
            return "redirect:/api/user/profile";
        } catch (RuntimeException e) {
            logger.error("Error cancelling booking: {}", bookingId, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/api/user/profile";
        }
    }

    /**
     * Get booking details by ID (for API/modal display)
     */
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id) {
        try {
            BookingDTO booking = bookingService.getBookingById(id);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            logger.error("Error fetching booking: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get booking details by ID for the current user (for user dashboard modal display)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/booking-details/{id}")
    @ResponseBody
    public ResponseEntity<BookingDTO> getUserBookingDetails(@PathVariable Long id, Authentication authentication) {
        try {
            // Get the current user
            String username = authentication.getName();
            User currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get the booking
            BookingDTO booking = bookingService.getBookingById(id);
            
            // Security check: ensure the booking belongs to the current user
            if (!booking.getUser().getId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to access booking {} belonging to another user", 
                           currentUser.getId(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            logger.error("Error fetching booking details for user: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get current user's bookings (API endpoint)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user/current")
    public ResponseEntity<List<BookingDTO>> getCurrentUserBookings(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<BookingDTO> bookings = bookingService.getBookingsByUserId(user.getId());
        return ResponseEntity.ok(bookings);
    }

    /**
     * AJAX update method (for RESERVATION_MANAGER dashboard)
     */
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @PostMapping("/update-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateBookingAjax(@RequestParam Long bookingId,
                                                                 @RequestParam Integer passengers,
                                                                 @RequestParam String status) {
        try {
            if (bookingId == null || bookingId <= 0) {
                logger.warn("Invalid bookingId provided for update: {}", bookingId);
                Map<String, String> response = new HashMap<>();
                response.put("error", "Invalid booking ID.");
                return ResponseEntity.badRequest().body(response);
            }

            BookingDTO updatedBookingDTO = new BookingDTO();
            updatedBookingDTO.setPassengers(passengers);
            updatedBookingDTO.setStatus(status);
            bookingService.updateBooking(bookingId, updatedBookingDTO);

            logger.info("Booking updated successfully via AJAX: {}", bookingId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Booking updated successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating booking via AJAX: {}", bookingId, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * AJAX delete method (for RESERVATION_MANAGER dashboard)
     */
    @PreAuthorize("hasRole('RESERVATION_MANAGER')")
    @PostMapping("/delete-ajax")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteBookingAjax(@RequestParam Long bookingId) {
        try {
            if (bookingId == null || bookingId <= 0) {
                logger.warn("Invalid bookingId provided for deletion: {}", bookingId);
                Map<String, String> response = new HashMap<>();
                response.put("error", "Invalid booking ID.");
                return ResponseEntity.badRequest().body(response);
            }

            bookingService.deleteBooking(bookingId);
            logger.info("Booking deleted successfully via AJAX: {}", bookingId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Booking deleted successfully.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error deleting booking via AJAX: {}", bookingId, e);
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}