package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
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
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/api/marketing")
@PreAuthorize("hasRole('MARKETING_EXECUTIVE')")
public class PromotionController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionController.class);

    @Autowired
    private PromotionService promotionService;
    @Autowired
    private UserRepository userRepository;

    private Date parseColomboDate(String value) throws ParseException {
        try {
            return Date.from(LocalDate.parse(value).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ParseException("Expected ISO calendar date", 0);
        }
    }

    @GetMapping("/promotions")
    public String viewPromotions(Model model) {
        try {
            List<PromotionDTO> promotions = promotionService.getAllPromotions();
            model.addAttribute("promotions", promotions);
            model.addAttribute("baseUrl", "/api/marketing");
            model.addAttribute("dashboardUrl", "/dashboard/marketing-manager");
            logger.info("Promotion list retrieved: {} promotions found", promotions.size());
            return "promotion-manager-dashboard";
        } catch (RuntimeException e) {
            logger.error("Error retrieving promotions", e);
            model.addAttribute("error", "Unable to load promotions. Please try again.");
            model.addAttribute("baseUrl", "/api/marketing");
            model.addAttribute("dashboardUrl", "/dashboard/marketing-manager");
            return "promotion-manager-dashboard";
        }
    }

    @GetMapping("/promotion/{id}")
    public String viewPromotion(@PathVariable Long id, Model model) {
        try {
            PromotionDTO promotion = promotionService.getPromotionById(id);
            model.addAttribute("promotion", promotion);
            model.addAttribute("baseUrl", "/api/marketing");
            logger.info("Promotion details retrieved: ID {}", id);
            return "promotion-details";
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
            model.addAttribute("baseUrl", "/api/marketing");
            logger.info("Editing promotion: ID {}", id);
            return "promotion-form";
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
                                  @RequestParam("scope") Promotion.Scope scope,
                                  Model model) {
        try {
            logger.info("Creating promotion with data: targetCriteria={}, discount={}, promoCode={}, validityStart={}, validityEnd={}",
                    targetCriteria, discount, promoCode, validityStartStr, validityEndStr);

            // Parse dates
            Date validityStart = parseColomboDate(validityStartStr);
            Date validityEnd = parseColomboDate(validityEndStr);

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
            promotionDTO.setScope(scope);

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
            Date validityStart = parseColomboDate(validityStartStr);
            Date validityEnd = parseColomboDate(validityEndStr);

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

    @GetMapping("/promotion/{id}/eligibility")
    public String eligibilityForm(@PathVariable Long id, Model model) {
        model.addAttribute("promotion", promotionService.getPromotionById(id));
        model.addAttribute("travelers", userRepository.findByRole(Role.FREQUENT_TRAVELER));
        model.addAttribute("baseUrl", "/api/marketing");
        return "promotion-eligibility";
    }

    @PostMapping("/promotion/{id}/eligibility")
    public String saveEligibility(@PathVariable Long id,
                                  @RequestParam("scope") Promotion.Scope scope,
                                  @RequestParam(name = "active", defaultValue = "false") boolean active,
                                  @RequestParam(name = "userIds", required = false) List<Long> userIds,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            promotionService.updateEligibility(id, scope, active, userIds);
            redirect.addFlashAttribute("success", "Promotion eligibility saved");
            return "redirect:/api/marketing/promotion/" + id + "/eligibility";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/api/marketing/promotion/" + id + "/eligibility";
        }
    }
}
