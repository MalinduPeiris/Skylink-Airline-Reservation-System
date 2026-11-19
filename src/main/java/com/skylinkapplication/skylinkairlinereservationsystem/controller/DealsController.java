package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.service.AuthService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/deals")
public class DealsController {

    private static final Logger logger = LoggerFactory.getLogger(DealsController.class);

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private AuthService authService;

    @GetMapping
    public String showDeals(Model model, HttpSession session) {
        try {
            logger.info("Loading deals page");

            // Initialize empty lists to prevent null pointer exceptions
            List<PromotionDTO> allPromotions = new ArrayList<>();

            try {
                Long userId = (Long) session.getAttribute("userId");
                User traveler = userId == null ? null : authService.getUserById(userId);
                allPromotions = promotionService.getVisiblePromotions(traveler);
            } catch (Exception e) {
                logger.error("Error fetching promotions", e);
            }

            model.addAttribute("allPromotions", allPromotions);

            // Get user-specific promotions if logged in
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) {
                try {
                    // Only explicit linked-user promotions are special offers.
                    List<PromotionDTO> specialPromotions = allPromotions.stream()
                            .filter(promo -> promo.getScope() == com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion.Scope.TARGETED)
                            .limit(4)
                            .collect(Collectors.toList());

                    model.addAttribute("specialPromotions", specialPromotions);
                    logger.info("Loaded {} special promotions for user ID: {}", specialPromotions.size(), userId);
                } catch (Exception e) {
                    logger.warn("Could not load special promotions for user ID: {}", userId, e);
                    model.addAttribute("specialPromotions", new ArrayList<>());
                }
            } else {
                model.addAttribute("specialPromotions", new ArrayList<>());
            }

            logger.info("Deals page loaded with {} total promotions", allPromotions.size());
            return "deals";

        } catch (Exception e) {
            logger.error("Error loading deals page", e);
            model.addAttribute("error", "Unable to load promotions at this time");
            model.addAttribute("allPromotions", new ArrayList<>());
            model.addAttribute("specialPromotions", new ArrayList<>());
            return "deals";
        }
    }

}
