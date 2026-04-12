package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FeedbackDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Feedback;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FeedbackRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public FeedbackDTO createFeedback(FeedbackDTO feedbackDTO) {
        // Validate required fields
        if (feedbackDTO.getRating() == null || feedbackDTO.getRating() < 1 || feedbackDTO.getRating() > 5) {
            throw new IllegalArgumentException("Invalid rating (must be between 1 and 5)");
        }

        if (feedbackDTO.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required to submit feedback.");
        }

        User user = userRepository.findById(feedbackDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + feedbackDTO.getUserId()));

        Feedback feedback = new Feedback();
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComments(feedbackDTO.getComments() != null ? feedbackDTO.getComments().trim() : null);
        feedback.setSubmittedDate(new Date());

        feedback.setUser(user);

        Feedback savedFeedback = feedbackRepository.save(feedback);
        return convertToDTO(savedFeedback);
    }

    @Transactional
    public FeedbackDTO updateFeedback(Long id, FeedbackDTO updatedFeedbackDTO) {
        // Validate ID
        if (id == null) {
            throw new IllegalArgumentException("Feedback ID is required");
        }

        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        // Validate and update fields if provided
        if (updatedFeedbackDTO.getRating() != null) {
            if (updatedFeedbackDTO.getRating() < 1 || updatedFeedbackDTO.getRating() > 5) {
                throw new IllegalArgumentException("Invalid rating (must be between 1 and 5)");
            }
            feedback.setRating(updatedFeedbackDTO.getRating());
        }
        
        if (updatedFeedbackDTO.getComments() != null) {
            feedback.setComments(updatedFeedbackDTO.getComments().trim());
        }

        Feedback updatedFeedback = feedbackRepository.save(feedback);
        return convertToDTO(updatedFeedback);
    }

    public FeedbackDTO getFeedbackById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Feedback ID is required");
        }
        
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        return convertToDTO(feedback);
    }

    public List<FeedbackDTO> getAllFeedbacks() {
        return feedbackRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFeedback(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Feedback ID is required");
        }
        
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        feedbackRepository.delete(feedback);
    }

    private FeedbackDTO convertToDTO(Feedback feedback) {
        FeedbackDTO dto = new FeedbackDTO();
        dto.setId(feedback.getId());
        dto.setRating(feedback.getRating());
        dto.setComments(feedback.getComments());
        dto.setSubmittedDate(feedback.getSubmittedDate());
        dto.setUserId(feedback.getUser() != null ? feedback.getUser().getId() : null);
        return dto;
    }
}