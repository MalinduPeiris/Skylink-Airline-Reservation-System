package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.UserDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.AuditLogger;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.NotificationService;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.NotificationService.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class AuthService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private final NotificationService notificationService = NotificationService.getInstance();
	private final AuditLogger auditLogger = AuditLogger.getInstance();

	@Transactional
	public UserDTO registerUser(UserDTO userDTO) {
		// Validate required fields
		if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
			throw new IllegalArgumentException("Username is required");
		}
		if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
			throw new IllegalArgumentException("Password is required");
		}
		if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Email is required");
		}
		if (userDTO.getRole() == null || userDTO.getRole().trim().isEmpty()) {
			throw new IllegalArgumentException("Role is required");
		}

		// Validate email format
		if (!isValidEmail(userDTO.getEmail())) {
			throw new IllegalArgumentException("Invalid email format");
		}

		// Validate password strength
		if (!isStrongPassword(userDTO.getPassword())) {
			throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one digit");
		}

		// Validate role
		try {
			Role.valueOf(userDTO.getRole());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid role: " + userDTO.getRole());
		}

		// Check if username already exists
		if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
			throw new RuntimeException("Username already exists");
		}
		
		// Check if email already exists
		if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
			throw new RuntimeException("Email already exists");
		}

		User user = new User();
		user.setUsername(userDTO.getUsername().trim());
		user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
		user.setEmail(userDTO.getEmail().toLowerCase().trim());
		user.setPhonenumber(userDTO.getPhonenumber());
		user.setAddress(userDTO.getAddress());
		user.setRole(Role.valueOf(userDTO.getRole()));

		User savedUser = userRepository.save(user);

		auditLogger.logAction(
				savedUser.getId().toString(),
				"USER_REGISTRATION",
				"User",
				"New user registered: " + savedUser.getUsername() + " with role: " + savedUser.getRole()
		);

		Notification emailNotification = new Notification(
				"EMAIL",
				savedUser.getEmail(),
				"Welcome to SkyLink Airline Reservation System",
				String.format("Dear %s,\n\nWelcome to SkyLink! Your account has been successfully created.\n\nUsername: %s\nRole: %s\n\nThank you for choosing SkyLink Airlines.",
						savedUser.getUsername(), savedUser.getUsername(), savedUser.getRole())
		);
		notificationService.addNotification(emailNotification);

		if (savedUser.getPhonenumber() != null && !savedUser.getPhonenumber().isEmpty()) {
			Notification smsNotification = new Notification(
					"SMS",
					savedUser.getPhonenumber(),
					"Account Created",
					String.format("Welcome %s! Your SkyLink account is ready. Login at skylink.com", savedUser.getUsername())
			);
			notificationService.addNotification(smsNotification);
		}

		Notification inAppNotification = new Notification(
				"IN_APP",
				savedUser.getUsername(),
				"Account Setup Complete",
				"Your profile has been created. Complete your profile settings to get started!"
		);
		notificationService.addNotification(inAppNotification);

		return convertToDTO(savedUser);
	}

	private UserDTO convertToDTO(User user) {
		UserDTO dto = new UserDTO();
		dto.setId(user.getId());
		dto.setUsername(user.getUsername());
		dto.setEmail(user.getEmail());
		dto.setPhonenumber(user.getPhonenumber());
		dto.setAddress(user.getAddress());
		dto.setRole(user.getRole().name());
		return dto;
	}

	@Transactional
	public User createUser(User user) {
		// Validate required fields
		if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
			throw new IllegalArgumentException("Username is required");
		}
		if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
			throw new IllegalArgumentException("Password is required");
		}
		if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Email is required");
		}
		if (user.getRole() == null) {
			throw new IllegalArgumentException("Role is required");
		}

		// Validate email format
		if (!isValidEmail(user.getEmail())) {
			throw new IllegalArgumentException("Invalid email format");
		}

		// Validate password strength
		if (!isStrongPassword(user.getPassword())) {
			throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one digit");
		}

		// Check if username already exists
		if (userRepository.findByUsername(user.getUsername()).isPresent()) {
			throw new RuntimeException("Username '" + user.getUsername() + "' already exists!");
		}

		// Check if email already exists
		if (userRepository.findByEmail(user.getEmail()).isPresent()) {
			throw new RuntimeException("Email '" + user.getEmail() + "' is already registered!");
		}

		user.setPassword(passwordEncoder.encode(user.getPassword()));

		User createdUser = userRepository.save(user);

		AuditLogger.getInstance().logAction(
				user.getId().toString(),
				"USER_REGISTRATION",
				"User-" + user.getUsername(),
				"New user registered"
		);

		return createdUser;
	}

	@Transactional
	public User updateUser(Long id, User updatedUser) {
		// Validate ID
		if (id == null) {
			throw new IllegalArgumentException("User ID is required");
		}

		// Validate required fields
		if (updatedUser.getUsername() == null || updatedUser.getUsername().trim().isEmpty()) {
			throw new IllegalArgumentException("Username is required");
		}
		if (updatedUser.getEmail() == null || updatedUser.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Email is required");
		}
		if (updatedUser.getRole() == null) {
			throw new IllegalArgumentException("Role is required");
		}

		// Validate email format
		if (!isValidEmail(updatedUser.getEmail())) {
			throw new IllegalArgumentException("Invalid email format");
		}

		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		// Check if username is being changed and if new username already exists
		if (!user.getUsername().equals(updatedUser.getUsername())) {
			if (userRepository.findByUsername(updatedUser.getUsername()).isPresent()) {
				throw new RuntimeException("Username '" + updatedUser.getUsername() + "' is already taken");
			}
		}

		// Check if email is being changed and if new email already exists
		if (!user.getEmail().equals(updatedUser.getEmail())) {
			if (userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
				throw new RuntimeException("Email '" + updatedUser.getEmail() + "' is already registered");
			}
		}

		user.setUsername(updatedUser.getUsername().trim());
		user.setEmail(updatedUser.getEmail().toLowerCase().trim());
		user.setPhonenumber(updatedUser.getPhonenumber());
		user.setAddress(updatedUser.getAddress());
		user.setRole(updatedUser.getRole());

		if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
			// Validate password strength if it's being updated
			if (!isStrongPassword(updatedUser.getPassword())) {
				throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one digit");
			}

			if (!updatedUser.getPassword().startsWith("$2a$") &&
					!updatedUser.getPassword().startsWith("$2b$") &&
					!updatedUser.getPassword().startsWith("$2y$")) {
				user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
			}
		}

		return userRepository.save(user);
	}

	public User getUserById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		
		return userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
	}

	public User getUserByUsername(String username) {
		if (username == null || username.trim().isEmpty()) {
			throw new IllegalArgumentException("Username is required");
		}
		
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + username));
	}

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@Transactional
	public void deleteUser(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		
		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));
		userRepository.delete(user);
	}

	public User login(String username, String password) {
		if (username == null || username.trim().isEmpty()) {
			throw new IllegalArgumentException("Username is required");
		}
		if (password == null || password.trim().isEmpty()) {
			throw new IllegalArgumentException("Password is required");
		}

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Invalid username or password"));

		if (passwordEncoder.matches(password, user.getPassword())) {
			return user;
		}
		throw new RuntimeException("Invalid username or password");
	}

	@Transactional
	public void resetPassword(String email, String newPassword) {
		if (email == null || email.trim().isEmpty()) {
			throw new IllegalArgumentException("Email is required");
		}
		if (newPassword == null || newPassword.trim().isEmpty()) {
			throw new IllegalArgumentException("New password is required");
		}

		// Validate password strength
		if (!isStrongPassword(newPassword)) {
			throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one digit");
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Email not found"));

		user.setPassword(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	public boolean isAuthorized(User user, String requiredRole) {
		return user != null && user.getRole().name().equals(requiredRole);
	}

	// Utility method to validate email format
	private boolean isValidEmail(String email) {
		if (email == null || email.trim().isEmpty()) {
			return false;
		}
		String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
		Pattern pattern = Pattern.compile(emailRegex);
		return pattern.matcher(email.trim()).matches();
	}

	// Utility method to validate password strength
	private boolean isStrongPassword(String password) {
		if (password == null || password.length() < 8) {
			return false;
		}

		boolean hasUppercase = false;
		boolean hasLowercase = false;
		boolean hasDigit = false;

		for (char c : password.toCharArray()) {
			if (Character.isUpperCase(c)) {
				hasUppercase = true;
			} else if (Character.isLowerCase(c)) {
				hasLowercase = true;
			} else if (Character.isDigit(c)) {
				hasDigit = true;
			}

			if (hasUppercase && hasLowercase && hasDigit) {
				return true;
			}
		}

		return hasUppercase && hasLowercase && hasDigit;
	}
}