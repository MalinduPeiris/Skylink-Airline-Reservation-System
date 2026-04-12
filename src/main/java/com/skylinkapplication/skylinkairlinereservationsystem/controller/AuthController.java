package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.UserDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.AuthService;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.UserRegistrationManager;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.AuditLogger;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String expired,
                                @RequestParam(required = false) String deleted,
                                Model model) {
        logger.info("Accessing login page");

        if (error != null) {
            model.addAttribute("error", "Invalid username or password. Please try again.");
        }

        if (logout != null) {
            model.addAttribute("success", "You have been logged out successfully.");
        }

        if (expired != null) {
            model.addAttribute("error", "Your session has expired. Please login again.");
        }

        if (deleted != null) {
            model.addAttribute("success", "Your account has been deleted successfully.");
        }

        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        logger.info("Accessing register page");
        model.addAttribute("userDTO", new UserDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute UserDTO userDTO,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        // Get singleton instance
        UserRegistrationManager registrationManager = UserRegistrationManager.getInstance();
        String identifier = userDTO.getEmail(); // Use email as identifier

        try {
            logger.info("Attempting to register user: {}", userDTO.getUsername());

            // STEP 1: Check if user is locked out
            if (registrationManager.isLockedOut(identifier)) {
                int failedAttempts = registrationManager.getFailedAttemptCount(identifier);
                logger.warn("Registration blocked - User locked out: {} (attempts: {})",
                        identifier, failedAttempts);
                model.addAttribute("error",
                        "Too many failed registration attempts. Please try again after 15 minutes.");
                model.addAttribute("userDTO", userDTO);
                return "register";
            }

            // STEP 2: Mark as pending registration
            registrationManager.markPendingRegistration(identifier);

            // STEP 3: Create User entity for validation
            User user = new User();
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setPassword(userDTO.getPassword());
            user.setPhonenumber(userDTO.getPhonenumber());
            user.setAddress(userDTO.getAddress());

            try {
                user.setRole(Role.valueOf(userDTO.getRole()));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid role provided: {}", userDTO.getRole());
                registrationManager.trackRegistrationAttempt(identifier, false);
                model.addAttribute("error", "Invalid role selected");
                model.addAttribute("userDTO", userDTO);
                return "register";
            }

            // STEP 4: Validate using singleton manager
            UserRegistrationManager.RegistrationValidationResult validationResult =
                    registrationManager.validateRegistration(user);

            if (!validationResult.isValid()) {
                logger.warn("Registration validation failed for {}: {}",
                        userDTO.getUsername(), validationResult.getErrorMessage());

                // Track failed attempt
                registrationManager.trackRegistrationAttempt(identifier, false);

                model.addAttribute("error", validationResult.getErrorMessage());
                model.addAttribute("userDTO", userDTO);
                return "register";
            }

            // STEP 5: Proceed with registration
            User createdUser = authService.createUser(user);
            userRepository.save(createdUser);

            // STEP 6: Track successful registration
            registrationManager.trackRegistrationAttempt(identifier, true);

            logger.info("User registered successfully: {}", createdUser.getUsername());

            // STEP 7: Generate confirmation token (optional - for email verification)
            String confirmationToken = registrationManager.generateConfirmationToken(createdUser.getUsername());
            logger.info("Confirmation token generated: {}", confirmationToken);

            // STEP 8: Send notification
            NotificationService.getInstance().addNotification(
                    new NotificationService.Notification(
                            "EMAIL",
                            createdUser.getEmail(),
                            "Welcome to Skylink Airlines",
                            "Your registration was successful! Welcome aboard, " + createdUser.getUsername()
                    )
            );

            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please login with your credentials.");
            return "redirect:/api/auth/login";

        } catch (RuntimeException e) {
            logger.error("Registration failed for {}: {}", userDTO.getUsername(), e.getMessage());

            // Track failed attempt
            registrationManager.trackRegistrationAttempt(identifier, false);

            // Get failed attempt count
            int failedAttempts = registrationManager.getFailedAttemptCount(identifier);
            String errorMessage = e.getMessage();

            if (failedAttempts >= 3) {
                errorMessage += String.format(" (Warning: %d failed attempts)", failedAttempts);
            }

            model.addAttribute("error", errorMessage);
            model.addAttribute("userDTO", userDTO);
            return "register";
        }
    }

    /**
     * JSON API endpoint - WITH SINGLETON INTEGRATION
     */
    @PostMapping("/register/api")
    @ResponseBody
    public ResponseEntity<?> registerUserApi(@RequestBody UserDTO userDTO) {

        UserRegistrationManager registrationManager = UserRegistrationManager.getInstance();
        String identifier = userDTO.getEmail();

        try {
            logger.info("API: Attempting to register user: {}", userDTO.getUsername());

            // Check if user is locked out
            if (registrationManager.isLockedOut(identifier)) {
                logger.warn("API: Registration blocked - User locked out: {}", identifier);
                return ResponseEntity.badRequest()
                        .body("Too many failed registration attempts. Please try again later.");
            }

            // Mark as pending
            registrationManager.markPendingRegistration(identifier);

            // Create and validate user
            User user = new User();
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setPassword(userDTO.getPassword());
            user.setPhonenumber(userDTO.getPhonenumber());
            user.setAddress(userDTO.getAddress());
            user.setRole(Role.valueOf(userDTO.getRole()));

            // Validate using singleton
            UserRegistrationManager.RegistrationValidationResult validationResult =
                    registrationManager.validateRegistration(user);

            if (!validationResult.isValid()) {
                registrationManager.trackRegistrationAttempt(identifier, false);
                return ResponseEntity.badRequest().body(validationResult.getErrorMessage());
            }

            // Create user
            User createdUser = authService.createUser(user);
            userRepository.save(createdUser);

            // Track success
            registrationManager.trackRegistrationAttempt(identifier, true);

            logger.info("API: User registered successfully: {}", createdUser.getUsername());

            return ResponseEntity.ok().body("User registered successfully");
        } catch (RuntimeException e) {
            logger.error("API: Registration failed: {}", e.getMessage());
            registrationManager.trackRegistrationAttempt(identifier, false);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/password-reset")
    public String showPasswordResetPage(Model model) {
        logger.info("Accessing password reset page");
        return "password-reset";
    }

    @PostMapping("/password-reset")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String newPassword,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            if (email == null || email.isEmpty() || newPassword == null || newPassword.isEmpty()) {
                logger.warn("Invalid input for password reset: email or password is empty");
                model.addAttribute("error", "Email and password are required.");
                return "password-reset";
            }

            authService.resetPassword(email, newPassword);

            // Log to audit
            AuditLogger.getInstance().logAction(
                    email,
                    "PASSWORD_RESET",
                    "User Account",
                    "Password reset successful"
            );

            logger.info("Password reset successful for email: {}", email);
            redirectAttributes.addFlashAttribute("success", "Password reset successfully. Please log in.");
            return "redirect:/api/auth/login";
        } catch (RuntimeException e) {
            logger.error("Password reset failed for email: {}", email, e);
            model.addAttribute("error", e.getMessage());
            return "password-reset";
        }
    }

    @GetMapping("/logout")
    public String logoutGet(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        return performLogout(request, redirectAttributes);
    }

    /**
     * POST logout - for form submission (recommended)
     */
    @PostMapping("/logout")
    public String logoutPost(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        return performLogout(request, redirectAttributes);
    }
//
//    @GetMapping("/logout")
//    public String logout() {
//        logger.info("User logout initiated");
//        SecurityContextHolder.clearContext();
//        return "redirect:/api/auth/login?logout=true";
//    }


    /**
     * Common logout logic
     */
    private String performLogout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Get username before clearing session
            HttpSession session = request.getSession(false);
            String username = null;
            if (session != null) {
                username = (String) session.getAttribute("username");
            }

            // Clear Spring Security context
            SecurityContextHolder.clearContext();

            // Invalidate session
            if (session != null) {
                session.invalidate();
            }

            logger.info("User logged out successfully: {}", username != null ? username : "Unknown");

            // Add success message
            redirectAttributes.addFlashAttribute("success", "You have been logged out successfully.");

            // Log audit trail
            if (username != null) {
                AuditLogger.getInstance().logAction(
                        username,
                        "USER_LOGOUT",
                        "Authentication",
                        "User logged out successfully"
                );
            }

            return "redirect:/api/auth/login?logout=true";
        } catch (Exception e) {
            logger.error("Error during logout", e);
            redirectAttributes.addFlashAttribute("error", "An error occurred during logout.");
            return "redirect:/api/auth/login";
        }
    }

    /**
     * BONUS: Admin endpoint to view registration statistics
     */
    @GetMapping("/registration-stats")
    @ResponseBody
    public ResponseEntity<?> getRegistrationStats() {
        UserRegistrationManager registrationManager = UserRegistrationManager.getInstance();
        UserRegistrationManager.RegistrationStatistics stats = registrationManager.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * BONUS: Admin endpoint to clear failed attempts
     */
    @PostMapping("/clear-failed-attempts")
    @ResponseBody
    public ResponseEntity<?> clearFailedAttempts(@RequestParam String identifier) {
        UserRegistrationManager registrationManager = UserRegistrationManager.getInstance();
        registrationManager.clearFailedAttempts(identifier);
        return ResponseEntity.ok("Failed attempts cleared for: " + identifier);
    }
}