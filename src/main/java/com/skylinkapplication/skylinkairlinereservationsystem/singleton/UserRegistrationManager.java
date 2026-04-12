package com.skylinkapplication.skylinkairlinereservationsystem.singleton;

import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Singleton class to manage user registration process
 * Provides centralized validation, tracking, and audit logging for user registration
 */
public class UserRegistrationManager {
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationManager.class);
    private static UserRegistrationManager instance;

    // Thread-safe collections for tracking registrations
    private final Map<String, RegistrationAttempt> registrationAttempts;
    private final Set<String> pendingRegistrations;
    private final Map<String, Integer> failedAttemptsCount;
    private final DateTimeFormatter formatter;

    // Configuration constants
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,50}$";
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_REGISTRATIONS_PER_HOUR = 10;

    // Statistics
    private int totalRegistrations = 0;
    private int successfulRegistrations = 0;
    private int failedRegistrations = 0;

    /**
     * Private constructor to prevent instantiation
     */
    private UserRegistrationManager() {
        this.registrationAttempts = new ConcurrentHashMap<>();
        this.pendingRegistrations = Collections.synchronizedSet(new HashSet<>());
        this.failedAttemptsCount = new ConcurrentHashMap<>();
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        logger.info("UserRegistrationManager singleton instance created");

        // Initialize cache cleanup thread
        startCleanupTask();
    }

    /**
     * Get singleton instance (thread-safe)
     */
    public static synchronized UserRegistrationManager getInstance() {
        if (instance == null) {
            instance = new UserRegistrationManager();
        }
        return instance;
    }

    /**
     * Validate user registration data before processing
     */
    public synchronized RegistrationValidationResult validateRegistration(User user) {
        RegistrationValidationResult result = new RegistrationValidationResult();

        try {
            // Check if user is locked out due to too many failed attempts
            if (isLockedOut(user.getUsername())) {
                result.setValid(false);
                result.addError("Account temporarily locked due to too many failed registration attempts. Please try again later.");
                return result;
            }

            // Validate username
            if (!validateUsername(user.getUsername())) {
                result.addError("Username must be 3-50 characters long and contain only letters, numbers, and underscores");
            }

            // Validate email
            if (!validateEmail(user.getEmail())) {
                result.addError("Invalid email format");
            }

            // Validate password
            if (!validatePassword(user.getPassword())) {
                result.addError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
            }

            // Validate phone number
            if (user.getPhonenumber() == null || user.getPhonenumber().trim().isEmpty()) {
                result.addError("Phone number is required");
            }

            // Validate address
            if (user.getAddress() == null || user.getAddress().trim().isEmpty()) {
                result.addError("Address is required");
            }

            // Validate role
            if (user.getRole() == null) {
                result.addError("Role selection is required");
            }

            // Check rate limiting
            if (!checkRateLimit(user.getEmail())) {
                result.addError("Too many registration attempts. Please try again later.");
            }

            result.setValid(result.getErrors().isEmpty());

            // Log validation attempt
            if (!result.isValid()) {
                AuditLogger.getInstance().logAction(
                        user.getEmail(),
                        "REGISTRATION_VALIDATION_FAILED",
                        "User Registration",
                        "Validation errors: " + String.join(", ", result.getErrors())
                );
            }

        } catch (Exception e) {
            logger.error("Error during registration validation", e);
            result.setValid(false);
            result.addError("An error occurred during validation. Please try again.");
        }

        return result;
    }

    /**
     * Track registration attempt
     */
    public synchronized void trackRegistrationAttempt(String identifier, boolean success) {
        totalRegistrations++;

        if (success) {
            successfulRegistrations++;
            // Clear failed attempts on success
            failedAttemptsCount.remove(identifier);
            registrationAttempts.remove(identifier);
            pendingRegistrations.remove(identifier);

            // Cache the successful registration
            CacheManager.getInstance().put("registration_success_" + identifier, true, 3600000);

            // Log to audit
            AuditLogger.getInstance().logAction(
                    identifier,
                    "USER_REGISTRATION_SUCCESS",
                    "User Registration",
                    "New user registered successfully"
            );

            logger.info("Successful registration tracked for: {}", identifier);
        } else {
            failedRegistrations++;

            // Increment failed attempts
            int attempts = failedAttemptsCount.getOrDefault(identifier, 0) + 1;
            failedAttemptsCount.put(identifier, attempts);

            // Record attempt details
            RegistrationAttempt attempt = new RegistrationAttempt(
                    identifier,
                    LocalDateTime.now(),
                    false
            );
            registrationAttempts.put(identifier, attempt);

            // Log to audit
            AuditLogger.getInstance().logAction(
                    identifier,
                    "USER_REGISTRATION_FAILED",
                    "User Registration",
                    "Failed registration attempt #" + attempts
            );

            logger.warn("Failed registration tracked for: {} (attempt #{})", identifier, attempts);

            // Check if user should be locked out
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                logger.warn("User {} locked out after {} failed attempts", identifier, attempts);
                NotificationService.getInstance().addNotification(
                        new NotificationService.Notification(
                                "SYSTEM_ALERT",
                                "admin@skylink.com",
                                "Multiple Failed Registration Attempts",
                                "User " + identifier + " has exceeded maximum registration attempts"
                        )
                );
            }
        }
    }

    /**
     * Mark registration as pending
     */
    public synchronized void markPendingRegistration(String identifier) {
        pendingRegistrations.add(identifier);
        logger.info("Registration marked as pending for: {}", identifier);
    }

    /**
     * Check if identifier is locked out
     */
    public synchronized boolean isLockedOut(String identifier) {
        int attempts = failedAttemptsCount.getOrDefault(identifier, 0);
        if (attempts < MAX_FAILED_ATTEMPTS) {
            return false;
        }

        // Check if lockout period has expired
        RegistrationAttempt lastAttempt = registrationAttempts.get(identifier);
        if (lastAttempt != null) {
            LocalDateTime lockoutExpiry = lastAttempt.getTimestamp()
                    .plusMinutes(LOCKOUT_DURATION_MINUTES);

            if (LocalDateTime.now().isAfter(lockoutExpiry)) {
                // Lockout period expired, reset attempts
                failedAttemptsCount.remove(identifier);
                registrationAttempts.remove(identifier);
                logger.info("Lockout period expired for: {}", identifier);
                return false;
            }
        }

        return true;
    }

    /**
     * Validate username format
     */
    private boolean validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return Pattern.matches(USERNAME_PATTERN, username);
    }

    /**
     * Validate email format
     */
    private boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Pattern.matches(EMAIL_PATTERN, email);
    }

    /**
     * Validate password strength
     */
    private boolean validatePassword(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * Check rate limiting for registrations
     */
    private synchronized boolean checkRateLimit(String identifier) {
        // Get recent registration attempts from cache
        String cacheKey = "registration_rate_" + identifier;
        Object cached = CacheManager.getInstance().get(cacheKey);

        if (cached instanceof Integer) {
            int count = (Integer) cached;
            if (count >= MAX_REGISTRATIONS_PER_HOUR) {
                logger.warn("Rate limit exceeded for: {}", identifier);
                return false;
            }
            CacheManager.getInstance().put(cacheKey, count + 1, 3600000); // 1 hour
        } else {
            CacheManager.getInstance().put(cacheKey, 1, 3600000);
        }

        return true;
    }

    /**
     * Get registration statistics
     */
    public synchronized RegistrationStatistics getStatistics() {
        return new RegistrationStatistics(
                totalRegistrations,
                successfulRegistrations,
                failedRegistrations,
                pendingRegistrations.size(),
                failedAttemptsCount.size()
        );
    }

    /**
     * Get failed attempt count for identifier
     */
    public synchronized int getFailedAttemptCount(String identifier) {
        return failedAttemptsCount.getOrDefault(identifier, 0);
    }

    /**
     * Clear failed attempts for identifier (admin function)
     */
    public synchronized void clearFailedAttempts(String identifier) {
        failedAttemptsCount.remove(identifier);
        registrationAttempts.remove(identifier);
        logger.info("Failed attempts cleared for: {}", identifier);

        AuditLogger.getInstance().logAction(
                "SYSTEM_ADMIN",
                "CLEAR_FAILED_ATTEMPTS",
                "User Registration",
                "Failed attempts cleared for: " + identifier
        );
    }

    /**
     * Start background cleanup task
     */
    private void startCleanupTask() {
        Timer cleanupTimer = new Timer(true);
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupExpiredAttempts();
            }
        }, 60000, 60000); // Run every minute
    }

    /**
     * Cleanup expired registration attempts
     */
    private synchronized void cleanupExpiredAttempts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = now.minusMinutes(LOCKOUT_DURATION_MINUTES);

        registrationAttempts.entrySet().removeIf(entry ->
                entry.getValue().getTimestamp().isBefore(expiryTime)
        );

        // Also cleanup cache
        CacheManager.getInstance().clearExpired();
    }

    /**
     * Generate registration confirmation token
     */
    public synchronized String generateConfirmationToken(String username) {
        String token = UUID.randomUUID().toString();
        CacheManager.getInstance().put("reg_token_" + token, username, 86400000); // 24 hours
        logger.info("Confirmation token generated for user: {}", username);
        return token;
    }

    /**
     * Validate confirmation token
     */
    public synchronized String validateConfirmationToken(String token) {
        Object cached = CacheManager.getInstance().get("reg_token_" + token);
        if (cached instanceof String) {
            return (String) cached;
        }
        return null;
    }

    /**
     * Inner class for registration validation result
     */
    public static class RegistrationValidationResult {
        private boolean valid;
        private final List<String> errors;

        public RegistrationValidationResult() {
            this.errors = new ArrayList<>();
            this.valid = true;
        }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getErrors() { return errors; }
        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    /**
     * Inner class for registration attempt tracking
     */
    private static class RegistrationAttempt {
        private final String identifier;
        private final LocalDateTime timestamp;
        private final boolean success;

        public RegistrationAttempt(String identifier, LocalDateTime timestamp, boolean success) {
            this.identifier = identifier;
            this.timestamp = timestamp;
            this.success = success;
        }

        public String getIdentifier() { return identifier; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
    }

    /**
     * Inner class for registration statistics
     */
    public static class RegistrationStatistics {
        private final int totalAttempts;
        private final int successfulAttempts;
        private final int failedAttempts;
        private final int pendingCount;
        private final int lockedOutCount;

        public RegistrationStatistics(int totalAttempts, int successfulAttempts,
                                      int failedAttempts, int pendingCount, int lockedOutCount) {
            this.totalAttempts = totalAttempts;
            this.successfulAttempts = successfulAttempts;
            this.failedAttempts = failedAttempts;
            this.pendingCount = pendingCount;
            this.lockedOutCount = lockedOutCount;
        }

        public int getTotalAttempts() { return totalAttempts; }
        public int getSuccessfulAttempts() { return successfulAttempts; }
        public int getFailedAttempts() { return failedAttempts; }
        public int getPendingCount() { return pendingCount; }
        public int getLockedOutCount() { return lockedOutCount; }
        public double getSuccessRate() {
            return totalAttempts > 0 ? (successfulAttempts * 100.0 / totalAttempts) : 0;
        }
    }
}