package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public User login(String username, String password) {
		User user = userRepository.findByUsername(username);
		if (user != null && passwordEncoder.matches(password, user.getPassword())) {
			return user;
		}
		throw new RuntimeException("Invalid username or password");
	}

	public void resetPassword(String email, String newPassword) {
		Optional<User> userOptional = userRepository.findAll().stream()
				.filter(u -> u.getEmail().equals(email))
				.findFirst();
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			user.setPassword(passwordEncoder.encode(newPassword));
			userRepository.save(user);
		} else {
			throw new RuntimeException("Email not found");
		}
	}

	public boolean isAuthorized(User user, String requiredRole) {
		return user != null && user.getRole().name().equals(requiredRole);
	}
}

