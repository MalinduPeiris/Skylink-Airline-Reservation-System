package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionValidityTest {
    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
    @Mock private PromotionRepository repository;
    @InjectMocks private PromotionService service;

    @Test
    void activeGeneralPromotionWorksForAnyAuthenticatedTravelerIncludingFinalDay() {
        Promotion promotion = promotion("SKY20", 0, 0);
        promotion.setScope(Promotion.Scope.GENERAL);
        promotion.setUsers(List.of(traveler(99))); // General authorization ignores linked users.
        when(repository.findByPromoCodeIgnoreCase("sky20")).thenReturn(Optional.of(promotion));
        var result = service.findRedeemablePromotion("sky20", traveler(8));
        assertTrue(result.isPresent());
        assertEquals(20.0, result.get().getDiscount());
    }

    @Test
    void targetedRequiresExactLinkedTravelerNotDescriptiveTargetCriteria() {
        Promotion promotion = promotion("VIP", -1, 1);
        promotion.setScope(Promotion.Scope.TARGETED);
        promotion.setTargetCriteria("everyone");
        promotion.setUsers(List.of(traveler(8)));
        when(repository.findByPromoCodeIgnoreCase("VIP")).thenReturn(Optional.of(promotion));
        assertTrue(service.findRedeemablePromotion("VIP", traveler(8)).isPresent());
        assertTrue(service.findRedeemablePromotion("VIP", traveler(9)).isEmpty());
    }

    @Test
    void nullScopeInactiveExpiredAndFuturePromotionsFailClosed() {
        Promotion promotion = promotion("CODE", -1, 1);
        promotion.setUsers(List.of(traveler(8)));
        when(repository.findByPromoCodeIgnoreCase("CODE")).thenReturn(Optional.of(promotion));
        assertTrue(service.findRedeemablePromotion("CODE", traveler(8)).isEmpty());
        promotion.setScope(Promotion.Scope.GENERAL);
        promotion.setActive(false);
        assertTrue(service.findRedeemablePromotion("CODE", traveler(8)).isEmpty());
        promotion.setActive(true);
        promotion.setValidityEnd(date(-1));
        assertTrue(service.findRedeemablePromotion("CODE", traveler(8)).isEmpty());
        promotion.setValidityStart(date(1));
        promotion.setValidityEnd(date(2));
        assertTrue(service.findRedeemablePromotion("CODE", traveler(8)).isEmpty());
    }

    @Test
    void invalidCodeAndMissingTravelerCannotBeRedeemed() {
        assertTrue(service.findRedeemablePromotion(" ", traveler(8)).isEmpty());
        assertTrue(service.findRedeemablePromotion("CODE", null).isEmpty());
        User staff = traveler(8);
        staff.setRole(Role.MARKETING_EXECUTIVE);
        assertTrue(service.findRedeemablePromotion("CODE", staff).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void publicAndTravelerListingsDoNotAdvertiseUnavailableTargetedCodes() {
        Promotion general = promotion("ALL", -1, 1);
        general.setScope(Promotion.Scope.GENERAL);
        general.setUsers(List.of());
        Promotion targeted = promotion("PRIVATE", -1, 1);
        targeted.setScope(Promotion.Scope.TARGETED);
        targeted.setUsers(List.of(traveler(8)));
        Promotion legacy = promotion("LEGACY", -1, 1);
        legacy.setUsers(List.of());
        when(repository.findAll()).thenReturn(List.of(general, targeted, legacy));

        assertEquals(List.of("ALL"), service.getVisiblePromotions(null).stream().map(p -> p.getPromoCode()).toList());
        assertEquals(List.of("ALL"), service.getVisiblePromotions(traveler(9)).stream().map(p -> p.getPromoCode()).toList());
        assertEquals(List.of("ALL", "PRIVATE"), service.getVisiblePromotions(traveler(8)).stream().map(p -> p.getPromoCode()).toList());
    }

    private static Promotion promotion(String code, int startDays, int endDays) {
        Promotion promotion = new Promotion();
        promotion.setPromoCode(code);
        promotion.setDiscount(20.0);
        promotion.setTargetCriteria("descriptive only");
        promotion.setActive(true);
        promotion.setValidityStart(date(startDays));
        promotion.setValidityEnd(date(endDays));
        return promotion;
    }

    private static Date date(int offsetDays) {
        return Date.from(LocalDate.now(COLOMBO).plusDays(offsetDays).atStartOfDay(COLOMBO).toInstant());
    }

    private static User traveler(long id) {
        User user = new User();
        user.setId(id);
        user.setRole(Role.FREQUENT_TRAVELER);
        return user;
    }
}
