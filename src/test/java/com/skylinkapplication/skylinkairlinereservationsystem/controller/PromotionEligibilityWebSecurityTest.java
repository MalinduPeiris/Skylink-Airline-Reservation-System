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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        mvc.perform(get("/api/marketing/promotions")).andExpect(status().isOk());
        mvc.perform(get("/api/marketing/promotion/19")).andExpect(status().isOk());
    }
}
