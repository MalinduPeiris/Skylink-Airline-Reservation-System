package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FeedbackDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.SupportTicketDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FeedbackService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.SupportService;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/api/user/support")
public class UserSupportController {

    private static final Logger logger = LoggerFactory.getLogger(UserSupportController.class);

    @Autowired
    private SupportService supportService;

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/tickets")
    public String viewTickets(@RequestParam(required = false) Long userId, Model model, Authentication authentication) {
        try {
            // Pre-populate form with user data if authenticated
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                model.addAttribute("currentUser", currentUser);
                // If userId parameter is not provided, use the authenticated user's ID
                if (userId == null) {
                    userId = currentUser.getId();
                }
            }
            
            // Pass userId to the view
            if (userId != null) {
                model.addAttribute("userId", userId);
            }

            logger.info("User support ticket page accessed");
            return "user-support-ticket";
        } catch (RuntimeException e) {
            logger.error("Error accessing user support ticket page", e);
            model.addAttribute("error", "Unable to load support page. Please try again.");
            return "error";
        }
    }

    @PostMapping("/submit-ticket")
    public String submitSupportTicket(@RequestParam String name,
                                      @RequestParam String email,
                                      @RequestParam String issueTitle,
                                      @RequestParam String issueDescription,
                                      @RequestParam(required = false) Long userId,
                                      Model model,
                                      Authentication authentication) {
        try {
            if (name == null || name.trim().isEmpty() ||
                    email == null || email.trim().isEmpty() ||
                    issueTitle == null || issueTitle.trim().isEmpty() ||
                    issueDescription == null || issueDescription.trim().isEmpty()) {
                logger.warn("Invalid support ticket data provided");
                model.addAttribute("error", "All fields are required.");
                return "redirect:/api/user/support/tickets(userId=${session.userId})";
            }

            SupportTicketDTO ticketDTO = new SupportTicketDTO();
            ticketDTO.setIssueTitle(issueTitle);
            ticketDTO.setIssueDescription(issueDescription);

            // Set user ID if provided or if authenticated
            Long effectiveUserId = userId;
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                // If userId wasn't provided in the request, use the authenticated user's ID
                if (effectiveUserId == null) {
                    effectiveUserId = currentUser.getId();
                }
            }
            
            if (effectiveUserId != null) {
                ticketDTO.setUserId(effectiveUserId);
            }

            supportService.createSupportTicket(ticketDTO);
            logger.info("Support ticket submitted successfully by user: {}", name);
            model.addAttribute("success", "Support ticket submitted successfully! We'll address it within 48 hours.");
            return "user-support-ticket";
        } catch (RuntimeException e) {
            logger.error("Error submitting support ticket", e);
            model.addAttribute("error", "Failed to submit support ticket: " + e.getMessage());
            return "user-support-ticket";
        }
    }

    @PostMapping("/submit-feedback")
    public String submitFeedback(@RequestParam String name,
                                 @RequestParam String email,
                                 @RequestParam Integer rating,
                                 @RequestParam(required = false) String comments,
                                 @RequestParam(required = false) Long userId,
                                 Model model,
                                 Authentication authentication) {
        try {
            if (name == null || name.trim().isEmpty() ||
                    email == null || email.trim().isEmpty() ||
                    rating == null || rating < 1 || rating > 5) {
                logger.warn("Invalid feedback data provided");
                model.addAttribute("error", "Name, email, and rating (1-5) are required.");
                return "user-support-ticket";
            }

            FeedbackDTO feedbackDTO = new FeedbackDTO();
            feedbackDTO.setRating(rating);
            feedbackDTO.setComments(comments != null ? comments : "");

            // Set user ID if provided or if authenticated
            Long effectiveUserId = userId;
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                // If userId wasn't provided in the request, use the authenticated user's ID
                if (effectiveUserId == null) {
                    effectiveUserId = currentUser.getId();
                }
            }
            
            if (effectiveUserId != null) {
                feedbackDTO.setUserId(effectiveUserId);
            }

            feedbackService.createFeedback(feedbackDTO);
            logger.info("Feedback submitted successfully by user: {}", name);
            model.addAttribute("success", "Thank you for your feedback! We appreciate you taking the time to share your experience with us.");
            return "user-support-ticket";
        } catch (RuntimeException e) {
            logger.error("Error submitting feedback", e);
            model.addAttribute("error", "Failed to submit feedback: " + e.getMessage());
            return "user-support-ticket";
        }
    }
}