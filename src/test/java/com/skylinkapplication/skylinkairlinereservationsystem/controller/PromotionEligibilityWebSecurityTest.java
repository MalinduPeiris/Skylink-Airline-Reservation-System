package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.config.SecurityConfig;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(PromotionController.class)
@Import(SecurityConfig.class)
class PromotionEligibilityWebSecurityTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PromotionService service;
    @MockitoBean private UserRepository users;
    @MockitoBean private AuthenticationProvider authenticationProvider;
    @MockitoBean private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Test
    void anonymousCannotOpenClassificationForm() throws Exception {
        mvc.perform(get("/api/marketing/promotion/19/eligibility"))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "traveler", roles = "FREQUENT_TRAVELER")
    void travelerCannotChangePromotionScopeEvenWithCsrf() throws Exception {
        mvc.perform(post("/api/marketing/promotion/19/eligibility").with(csrf())
                        .param("scope", "GENERAL").param("active", "true"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "marketing", roles = "MARKETING_EXECUTIVE")
    void marketingChangeRequiresCsrf() throws Exception {
        mvc.perform(post("/api/marketing/promotion/19/eligibility")
                        .param("scope", "GENERAL").param("active", "true"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "marketing", roles = "MARKETING_EXECUTIVE")
    void marketingCanSubmitExplicitClassification() throws Exception {
        mvc.perform(post("/api/marketing/promotion/19/eligibility").with(csrf())
                        .param("scope", "TARGETED").param("active", "true").param("userIds", "7"))
                .andExpect(status().is3xxRedirection());
        verify(service).updateEligibility(19L, Promotion.Scope.TARGETED, true, List.of(7L));
    }

    @Test
    @WithMockUser(username = "marketing", roles = "MARKETING_EXECUTIVE")
    void marketingCanRenderLegacyClassificationForm() throws Exception {
        PromotionDTO promotion = new PromotionDTO();
        promotion.setId(19L);
        promotion.setPromoCode("LEGACY");
        promotion.setEligibleUserIds(List.of());
        when(service.getPromotionById(19L)).thenReturn(promotion);
        when(users.findByRole(com.skylinkapplication.skylinkairlinereservationsystem.model.Role.FREQUENT_TRAVELER))
                .thenReturn(List.of());
        mvc.perform(get("/api/marketing/promotion/19/eligibility"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "marketing", roles = "MARKETING_EXECUTIVE")
    void marketingDashboardAndDetailsRenderPromotionStatus() throws Exception {
        PromotionDTO promotion = new PromotionDTO();
        promotion.setId(19L);
        promotion.setPromoCode("LEGACY");
        promotion.setDiscount(20.0);
        promotion.setTargetCriteria("descriptive");
        promotion.setValidityStart(new Date());
        promotion.setValidityEnd(new Date());
        promotion.setEligibleUserIds(List.of());
        when(service.getAllPromotions()).thenReturn(List.of(promotion));
        when(service.getPromotionById(19L)).thenReturn(promotion);
        mvc.perform(get("/api/marketing/promotions"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1 class=\"page-title\">")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"promoCode\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"promoCode\"")));
        mvc.perform(get("/api/marketing/promotion/19")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "marketing", roles = "MARKETING_EXECUTIVE")
    void marketingDashboardCountsOnlyRedeemableCampaignsAsActive() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Colombo"));
        PromotionDTO available = promotion(1L, Promotion.Scope.GENERAL, true, today.minusDays(1), today);
        PromotionDTO expired = promotion(2L, Promotion.Scope.GENERAL, true, today.minusDays(3), today.minusDays(1));
        PromotionDTO unclassified = promotion(3L, null, true, today.minusDays(1), today.plusDays(1));
        available.setDiscount(20.0);
        expired.setDiscount(90.0);
        when(service.getAllPromotions()).thenReturn(List.of(available, expired, unclassified));

        mvc.perform(get("/api/marketing/promotions"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<div class=\"metric-value\">1</div>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("3 total")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("20.0%")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Expiring Soon"))));
    }

    private static PromotionDTO promotion(Long id, Promotion.Scope scope, boolean active,
                                          LocalDate start, LocalDate end) {
        PromotionDTO dto = new PromotionDTO();
        dto.setId(id);
        dto.setPromoCode("TEST" + id);
        dto.setDiscount(10.0);
        dto.setTargetCriteria("Synthetic promotion");
        dto.setScope(scope);
        dto.setActive(active);
        dto.setValidityStart(Date.from(start.atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant()));
        dto.setValidityEnd(Date.from(end.atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant()));
        return dto;
    }
}
