package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.service.AuthService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/profile")
    public String showProfile(Model model, HttpSession session, Authentication authentication) {
        try {
            // Get authenticated user
            String username;
            if (authentication != null && authentication.isAuthenticated() &&
                    !"anonymousUser".equals(authentication.getName())) {
                username = authentication.getName();
            } else {
                username = (String) session.getAttribute("username");
            }

            if (username == null) {
                logger.warn("No authenticated user found");
                return "redirect:/api/auth/login?error=Please login";
            }

            // Fetch user by username
            User user = authService.getUserByUsername(username);
            if (user == null) {
                logger.error("User not found with username: {}", username);
                return "redirect:/api/auth/login?error=User not found";
            }

            // Update session
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("role", user.getRole().name());

            // Get user's bookings
            List<BookingDTO> bookingDTOs = new ArrayList<>();
            try {
                bookingDTOs = bookingService.getBookingsByUserId(user.getId());
            } catch (Exception e) {
                logger.error("Error retrieving bookings for user ID: {}", user.getId(), e);
                bookingDTOs = new ArrayList<>();
            }

            Date now = new Date();

            // Separate upcoming and past bookings
            List<BookingDTO> upcomingBookings = bookingDTOs.stream()
                    .filter(b -> b.getFlight() != null &&
                            b.getFlight().getDepartureDate() != null &&
                            b.getFlight().getDepartureDate().after(now) &&
                            ("PENDING".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus())))
                    .sorted(Comparator.comparing(b -> b.getFlight().getDepartureDate()))
                    .collect(Collectors.toList());

            List<BookingDTO> pastBookings = bookingDTOs.stream()
                    .filter(b -> b.getFlight() != null &&
                            b.getFlight().getDepartureDate() != null &&
                            (b.getFlight().getDepartureDate().before(now) || "CANCELLED".equals(b.getStatus())))
                    .sorted(Comparator.comparing(b -> b.getFlight().getDepartureDate(), Comparator.reverseOrder()))
                    .collect(Collectors.toList());

            // Calculate stats
            int totalBookings = bookingDTOs.size();
            int upcomingTrips = upcomingBookings.size();
            int completedTrips = (int) pastBookings.stream()
                    .filter(b -> "CONFIRMED".equals(b.getStatus()))
                    .count();

            Set<String> uniqueDestinations = bookingDTOs.stream()
                    .filter(b -> b.getFlight() != null && b.getFlight().getDestination() != null)
                    .map(b -> b.getFlight().getDestination())
                    .collect(Collectors.toSet());
            int destinations = uniqueDestinations.size();

            // Add attributes to model
            model.addAttribute("user", user);
            model.addAttribute("upcomingBookings", upcomingBookings);
            model.addAttribute("pastBookings", pastBookings);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("upcomingTrips", upcomingTrips);
            model.addAttribute("completedTrips", completedTrips);
            model.addAttribute("destinations", destinations);

            logger.info("Profile page loaded successfully for user: {} (ID: {}) with {} bookings",
                    user.getUsername(), user.getId(), totalBookings);
            return "user-dashboard";

        } catch (Exception e) {
            logger.error("Error loading profile page", e);
            model.addAttribute("error", "Unable to load profile: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Get booking details for the current user (AJAX endpoint)
     */
    @GetMapping("/booking-details/{id}")
    @ResponseBody
    public ResponseEntity<BookingDTO> getUserBookingDetails(
            @PathVariable Long id,
            Authentication authentication,
            HttpSession session) {
        try {
            // Get current user
            String username = authentication != null ? authentication.getName() :
                    (String) session.getAttribute("username");

            if (username == null) {
                logger.warn("No authenticated user for booking details request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User currentUser = authService.getUserByUsername(username);
            if (currentUser == null) {
                logger.warn("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Get the booking
            BookingDTO booking = bookingService.getBookingById(id);

            // Security check: ensure the booking belongs to the current user
            if (booking.getUserId() == null || !booking.getUserId().equals(currentUser.getId())) {
                logger.warn("User {} attempted to access booking {} belonging to another user",
                        currentUser.getId(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            logger.info("Booking details retrieved successfully: ID={}, User={}", id, username);
            return ResponseEntity.ok(booking);

        } catch (RuntimeException e) {
            logger.error("Error fetching booking details: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam Long id,
                                @RequestParam String username,
                                @RequestParam String email,
                                @RequestParam String phonenumber,
                                @RequestParam String address,
                                @RequestParam(required = false) String password,
                                RedirectAttributes redirectAttributes,
                                HttpSession session,
                                Authentication authentication) {
        try {
            // Get current user ID from session or authentication
            Long currentUserId = (Long) session.getAttribute("userId");

            if (currentUserId == null && authentication != null) {
                User user = authService.getUserByUsername(authentication.getName());
                currentUserId = user.getId();
            }

            // Security check
            if (currentUserId == null || !currentUserId.equals(id)) {
                logger.warn("Unauthorized profile update attempt. Session userId: {}, Request userId: {}",
                        currentUserId, id);
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/api/user/profile";
            }

            // Validate input
            if (username == null || username.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Username is required");
                return "redirect:/api/user/profile";
            }

            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email is required");
                return "redirect:/api/user/profile";
            }

            // Get current user
            User currentUser = authService.getUserById(id);

            // Create updated user object
            User updatedUser = new User();
            updatedUser.setId(id);
            updatedUser.setUsername(username);
            updatedUser.setEmail(email);
            updatedUser.setPhonenumber(phonenumber);
            updatedUser.setAddress(address);
            updatedUser.setRole(currentUser.getRole());

            // Handle password update
            if (password != null && !password.trim().isEmpty()) {
                if (password.length() < 6) {
                    redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
                    return "redirect:/api/user/profile";
                }
                updatedUser.setPassword(password);
            } else {
                updatedUser.setPassword(null); // Keep existing password
            }

            // Update user
            User savedUser = authService.updateUser(id, updatedUser);
            logger.info("Profile updated successfully for user ID: {} (new username: {})", id, username);

            // Update session with new values
            session.setAttribute("userId", savedUser.getId());
            session.setAttribute("username", savedUser.getUsername());
            session.setAttribute("email", savedUser.getEmail());
            session.setAttribute("role", savedUser.getRole().name());

            // Update Spring Security context if username changed
            if (!currentUser.getUsername().equals(savedUser.getUsername())) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    Authentication newAuth = new UsernamePasswordAuthenticationToken(
                            savedUser.getUsername(),
                            auth.getCredentials(),
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                    logger.info("Security context updated with new username: {}", savedUser.getUsername());
                }
            }

            redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
            return "redirect:/api/user/profile";

        } catch (RuntimeException e) {
            logger.error("Error updating profile for user ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error updating profile: " + e.getMessage());
            return "redirect:/api/user/profile";
        }
    }

    @PostMapping("/delete")
    public String deleteProfile(@RequestParam Long id,
                                RedirectAttributes redirectAttributes,
                                HttpSession session,
                                Authentication authentication) {
        try {
            // Get current user ID from session or authentication
            Long currentUserId = (Long) session.getAttribute("userId");

            if (currentUserId == null && authentication != null) {
                User user = authService.getUserByUsername(authentication.getName());
                currentUserId = user.getId();
            }

            // Security check
            if (currentUserId == null || !currentUserId.equals(id)) {
                logger.warn("Unauthorized profile deletion attempt. Session userId: {}, Request userId: {}",
                        currentUserId, id);
                redirectAttributes.addFlashAttribute("error", "Unauthorized access");
                return "redirect:/api/user/profile";
            }

            // Delete user
            authService.deleteUser(id);
            logger.info("User account deleted: ID {}", id);

            // Clear session and security context
            session.invalidate();
            SecurityContextHolder.clearContext();

            redirectAttributes.addFlashAttribute("success", "Account deleted successfully");
            return "redirect:/api/auth/login?deleted=true";

        } catch (RuntimeException e) {
            logger.error("Error deleting profile for user ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Error deleting account: " + e.getMessage());
            return "redirect:/api/user/profile";
        }
    }
}