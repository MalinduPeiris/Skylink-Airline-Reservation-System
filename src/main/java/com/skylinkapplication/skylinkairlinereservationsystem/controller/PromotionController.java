package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
@RequestMapping("/api/marketing")
@PreAuthorize("hasRole('MARKETING_EXECUTIVE')")
public class PromotionController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionController.class);

    @Autowired
    private PromotionService promotionService;

    @GetMapping("/promotions")
    public String viewPromotions(Model model) {
        try {
            List<PromotionDTO> promotions = promotionService.getAllPromotions();
            model.addAttribute("promotions", promotions);
            logger.info("Promotion list retrieved: {} promotions found", promotions.size());
            return "promotion-manager-dashboard"; // Returns the marketing manager dashboard template
        } catch (RuntimeException e) {
            logger.error("Error retrieving promotions", e);
            model.addAttribute("error", "Unable to load promotions. Please try again.");
            return "error";
        }
    }

    @GetMapping("/promotion/{id}")
    public String viewPromotion(@PathVariable Long id, Model model) {
        try {
            PromotionDTO promotion = promotionService.getPromotionById(id);
            model.addAttribute("promotion", promotion);
            logger.info("Promotion details retrieved: ID {}", id);
            return "promotion-details"; // Returns Thymeleaf template for promotion details
        } catch (RuntimeException e) {
            logger.error("Error retrieving promotion with ID: {}", id, e);
            model.addAttribute("error", "Unable to load promotion details. Please try again.");
            return "error";
        }
    }

    @GetMapping("/promotion/{id}/edit")
    public String editPromotionForm(@PathVariable Long id, Model model) {
        try {
            PromotionDTO promotion = promotionService.getPromotionById(id);
            model.addAttribute("promotion", promotion);
            logger.info("Editing promotion: ID {}", id);
            return "promotion-form"; // Returns Thymeleaf template for promotion edit form
        } catch (RuntimeException e) {
            logger.error("Error retrieving promotion for editing with ID: {}", id, e);
            model.addAttribute("error", "Unable to load promotion for editing. Please try again.");
            return "error";
        }
    }

    @PostMapping("/create")
    public String createPromotion(@RequestParam("targetCriteria") String targetCriteria,
                                  @RequestParam("discount") Double discount,
                                  @RequestParam("promoCode") String promoCode,
                                  @RequestParam("validityStart") String validityStartStr,
                                  @RequestParam("validityEnd") String validityEndStr,
                                  Model model) {
        try {
            logger.info("Creating promotion with data: targetCriteria={}, discount={}, promoCode={}, validityStart={}, validityEnd={}",
                    targetCriteria, discount, promoCode, validityStartStr, validityEndStr);

            // Parse dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date validityStart = dateFormat.parse(validityStartStr);
            Date validityEnd = dateFormat.parse(validityEndStr);

            // Validate input
            if (targetCriteria == null || targetCriteria.trim().isEmpty() ||
                    discount == null || discount <= 0 ||
                    promoCode == null || promoCode.trim().isEmpty()) {
                logger.warn("Invalid promotion data provided for creation");
                model.addAttribute("error", "Target criteria, promo code, and discount (positive value) are required.");
                return "promotion-form";
            }

            // Create DTO with the form data
            PromotionDTO promotionDTO = new PromotionDTO();
            promotionDTO.setTargetCriteria(targetCriteria);
            promotionDTO.setDiscount(discount);
            promotionDTO.setPromoCode(promoCode);
            promotionDTO.setValidityStart(validityStart);
            promotionDTO.setValidityEnd(validityEnd);

            PromotionDTO createdPromotion = promotionService.createPromotion(promotionDTO);
            logger.info("Promotion created successfully: {}", createdPromotion.getDiscount());
            return "redirect:/dashboard/marketing-manager?success=Promotion created successfully.";
        } catch (ParseException e) {
            logger.error("Error parsing dates for promotion creation", e);
            model.addAttribute("error", "Invalid date format. Please use YYYY-MM-DD format.");
            return "promotion-form";
        } catch (RuntimeException e) {
            logger.error("Error creating promotion", e);
            model.addAttribute("error", e.getMessage());
            return "promotion-form";
        }
    }

    @PostMapping("/update/{id}")
    public String updatePromotion(@PathVariable Long id,
                                  @RequestParam("targetCriteria") String targetCriteria,
                                  @RequestParam("discount") Double discount,
                                  @RequestParam("promoCode") String promoCode,
                                  @RequestParam("validityStart") String validityStartStr,
                                  @RequestParam("validityEnd") String validityEndStr,
                                  Model model) {
        try {
            logger.info("Updating promotion ID {}: targetCriteria={}, discount={}, promoCode={}, validityStart={}, validityEnd={}",
                    id, targetCriteria, discount, promoCode, validityStartStr, validityEndStr);

            // Parse dates
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date validityStart = dateFormat.parse(validityStartStr);
            Date validityEnd = dateFormat.parse(validityEndStr);

            // Create DTO with the form data
            PromotionDTO updatedPromotionDTO = new PromotionDTO();
            updatedPromotionDTO.setId(id);
            updatedPromotionDTO.setTargetCriteria(targetCriteria);
            updatedPromotionDTO.setDiscount(discount);
            updatedPromotionDTO.setPromoCode(promoCode);
            updatedPromotionDTO.setValidityStart(validityStart);
            updatedPromotionDTO.setValidityEnd(validityEnd);

            PromotionDTO updatedPromotion = promotionService.updatePromotion(id, updatedPromotionDTO);
            logger.info("Promotion updated successfully: ID {}", id);
            return "redirect:/dashboard/marketing-manager?success=Promotion updated successfully.";
        } catch (ParseException e) {
            logger.error("Error parsing dates for promotion update with ID: {}", id, e);
            return "redirect:/dashboard/marketing-manager?error=Invalid date format. Please use YYYY-MM-DD format.";
        } catch (RuntimeException e) {
            logger.error("Error updating promotion with ID: {}", id, e);
            return "redirect:/dashboard/marketing-manager?error=Unable to update promotion. Please try again.";
        }
    }

    @PostMapping("/delete/{id}")
    public String deletePromotion(@PathVariable Long id, Model model) {
        try {
            promotionService.deletePromotion(id);
            logger.info("Promotion deleted successfully: ID {}", id);
            return "redirect:/dashboard/marketing-manager?success=Promotion deleted successfully.";
        } catch (RuntimeException e) {
            logger.error("Error deleting promotion with ID: {}", id, e);
            return "redirect:/dashboard/marketing-manager?error=Unable to delete promotion. Please try again.";
        }
    }
}