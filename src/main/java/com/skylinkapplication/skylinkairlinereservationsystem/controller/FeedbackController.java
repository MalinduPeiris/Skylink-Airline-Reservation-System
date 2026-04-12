package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FeedbackDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/api/feedback")
@PreAuthorize("hasRole('RESERVATION_MANAGER') or hasRole('CUSTOMER_SUPPORT_OFFICER')")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private FeedbackService feedbackService;

//    @GetMapping("/form")
//    public String showFeedbackForm(Model model) {
//        logger.info("Accessing feedback form");
//        model.addAttribute("feedback", new FeedbackDTO());
//        return "feedback-form";
//    }

//    @PostMapping("/submit")
//    public String submitFeedback(@ModelAttribute FeedbackDTO feedbackDTO, Model model) {
//        try {
//            if (feedbackDTO == null || feedbackDTO.getUserId() == null || feedbackDTO.getComments() == null || feedbackDTO.getComments().isEmpty()) {
//                logger.warn("Invalid feedback data provided for submission: userId or feedbackText missing");
//                model.addAttribute("error", "User ID and feedback text are required.");
//                model.addAttribute("feedback", feedbackDTO != null ? feedbackDTO : new FeedbackDTO());
//                return "feedback-form";
//            }
//            feedbackService.createFeedback(feedbackDTO);
//            logger.info("Feedback submitted successfully for userId: {}", feedbackDTO.getUserId());
//            model.addAttribute("success", "Feedback submitted successfully.");
//            return "redirect:/api/booking/dashboard?userId=" + feedbackDTO.getUserId();
//        } catch (RuntimeException e) {
//            logger.error("Error submitting feedback for userId: {}", feedbackDTO != null ? feedbackDTO.getUserId() : "unknown", e);
//            model.addAttribute("error", "Unable to submit feedback. Please try again.");
//            model.addAttribute("feedback", feedbackDTO != null ? feedbackDTO : new FeedbackDTO());
//            return "feedback-form";
//        }
//    }

    @GetMapping("/list")
    public String listFeedbacks(Model model) {
        try {
            List<FeedbackDTO> feedbacks = feedbackService.getAllFeedbacks();
            model.addAttribute("feedbacks", feedbacks);
            logger.info("Retrieved {} feedbacks", feedbacks.size());
            return "feedback-list";
        } catch (RuntimeException e) {
            logger.error("Error retrieving feedbacks", e);
            model.addAttribute("error", "Unable to load feedbacks. Please try again.");
            return "error";
        }
    }

    @PostMapping("/update/{id}")
    public String updateFeedback(@PathVariable Long id,
                                 @RequestParam Integer rating,
                                 @RequestParam(required = false, defaultValue = "") String comments,
                                 RedirectAttributes redirectAttributes) {
        try {
            logger.info("Attempting to update feedback - ID: {}, Rating: {}, Comments: {}", id, rating, comments);

            // Validation
            if (id == null || id <= 0) {
                logger.warn("Invalid feedback ID: {}", id);
                redirectAttributes.addFlashAttribute("error", "Invalid feedback ID");
                return "redirect:/dashboard/customer-support";
            }

            if (rating == null || rating < 1 || rating > 5) {
                logger.warn("Invalid rating value: {}", rating);
                redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5");
                return "redirect:/dashboard/customer-support";
            }

            // Fetch existing feedback first to preserve user relationship
            FeedbackDTO existingFeedback = feedbackService.getFeedbackById(id);

            // Create DTO with updates
            FeedbackDTO feedbackDTO = new FeedbackDTO();
            feedbackDTO.setId(id);
            feedbackDTO.setRating(rating);
            feedbackDTO.setComments(comments != null ? comments.trim() : "");
            feedbackDTO.setUserId(existingFeedback.getUserId()); // Preserve user ID
            feedbackDTO.setSubmittedDate(existingFeedback.getSubmittedDate()); // Preserve date

            feedbackService.updateFeedback(id, feedbackDTO);
            logger.info("Feedback updated successfully - ID: {}, Rating: {}", id, rating);

            redirectAttributes.addFlashAttribute("success", "Feedback updated successfully!");
            return "redirect:/dashboard/customer-support";

        } catch (RuntimeException e) {
            logger.error("Error updating feedback ID: {} - {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update feedback: " + e.getMessage());
            return "redirect:/dashboard/customer-support";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            logger.info("Attempting to delete feedback - ID: {}", id);

            // Validation
            if (id == null || id <= 0) {
                logger.warn("Invalid feedback ID for deletion: {}", id);
                redirectAttributes.addFlashAttribute("error", "Invalid feedback ID");
                return "redirect:/dashboard/customer-support";
            }

            feedbackService.deleteFeedback(id);
            logger.info("Feedback deleted successfully - ID: {}", id);

            redirectAttributes.addFlashAttribute("success", "Feedback deleted successfully!");
            return "redirect:/dashboard/customer-support";

        } catch (RuntimeException e) {
            logger.error("Error deleting feedback ID: {} - {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete feedback: " + e.getMessage());
            return "redirect:/dashboard/customer-support";
        }
    }
}