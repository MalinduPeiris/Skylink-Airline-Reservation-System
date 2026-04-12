package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FeedbackDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/form")
    public String showFeedbackForm(Model model) {
        model.addAttribute("feedback", new FeedbackDTO());
        return "feedback-form";
    }

    @PostMapping("/submit")
    public String submitFeedback(@ModelAttribute FeedbackDTO feedbackDTO, Model model) {
        feedbackService.submitFeedback(feedbackDTO);
        return "redirect:/api/booking/dashboard?userId=" + feedbackDTO.getUserId();
    }
}