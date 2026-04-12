package com.skylinkapplication.skylinkairlinereservationsystem.config;

import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Get the authenticated username
        String username = authentication.getName();

        // Fetch the complete user from database
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            // Store user details in session
            HttpSession session = request.getSession();
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("email", user.getEmail());
            session.setAttribute("role", user.getRole().name());
            session.setAttribute("userObject", user);

            // Log successful login
            System.out.println("User logged in successfully: " + username + " (ID: " + user.getId() + ")");

            // Role-based redirect mapping
            String redirectUrl = "/?login=success"; // default for safety
            switch (user.getRole()) {
                case FREQUENT_TRAVELER:
                    redirectUrl = "/";
                    break;
                case RESERVATION_MANAGER:
                    // Route served by DashboardController
                    redirectUrl = "/dashboard/reservation-manager";
                    break;
                case FINANCE_EXECUTIVE:
                    redirectUrl = "/dashboard/finance";
                    break;
                case IT_SYSTEM_ENGINEER:
                    redirectUrl = "/dashboard/it-system-engineer/dashboard";
                    break;
                case MARKETING_EXECUTIVE:
                    redirectUrl = "/dashboard/marketing-manager";
                    break;
                case CUSTOMER_SUPPORT_OFFICER:
                    redirectUrl = "/dashboard/customer-support";
                    break;
                default:
                    redirectUrl = "/";
            }

			response.sendRedirect(redirectUrl);
            return;
        }

		// Fallback if user lookup failed
		response.sendRedirect("/?login=success");
    }
}