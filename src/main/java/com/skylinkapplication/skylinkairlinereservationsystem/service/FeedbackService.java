package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FeedbackDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Feedback;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    public void submitFeedback(FeedbackDTO feedbackDTO) {
        Feedback feedback = new Feedback();
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComments(feedbackDTO.getComments());
        feedback.setSubmittedDate(new Date());
        // Set user (to be implemented with proper mapping)
        feedbackRepository.save(feedback);
    }
}