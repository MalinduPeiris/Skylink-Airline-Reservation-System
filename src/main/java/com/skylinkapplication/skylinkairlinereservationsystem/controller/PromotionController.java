package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/marketing")
public class PromotionController {

    @Autowired
    private PromotionService promotionService;

    @GetMapping("/promotions")
    public String viewPromotions(Model model) {
        List<PromotionDTO> promotions = promotionService.getAllPromotions();
        model.addAttribute("promotions", promotions);
        return "promotion-management";
    }

    @PostMapping("/create")
    public String createPromotion(@ModelAttribute PromotionDTO promotionDTO, Model model) {
        promotionService.createPromotion(promotionDTO);
        return "redirect:/api/marketing/promotions";
    }
}