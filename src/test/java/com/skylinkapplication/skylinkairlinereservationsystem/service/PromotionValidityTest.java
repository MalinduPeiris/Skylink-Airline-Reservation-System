package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionValidityTest {
    @Mock private PromotionRepository repository;
    @InjectMocks private PromotionService service;

    @Test
    void matchingCodeWithinStoredDatesReturnsItsDiscount() {
        when(repository.findAll()).thenReturn(List.of(promotion("SKY20", -1, 1)));

        var result = service.findActivePromotion("sky20");

        assertTrue(result.isPresent());
        assertEquals(20.0, result.get().getDiscount());
    }

    @Test
    void expiredAndFuturePromotionsAreNotApplicable() {
        when(repository.findAll()).thenReturn(List.of(
                promotion("OLD", -3, -1), promotion("FUTURE", 1, 3)));

        assertTrue(service.findActivePromotion("OLD").isEmpty());
        assertTrue(service.findActivePromotion("FUTURE").isEmpty());
        assertTrue(service.findActivePromotion("MISSING").isEmpty());
    }

    private static Promotion promotion(String code, int startDays, int endDays) {
        Promotion promotion = new Promotion();
        promotion.setPromoCode(code);
        promotion.setDiscount(20.0);
        promotion.setTargetCriteria("Existing promotion");
        promotion.setValidityStart(Date.from(Instant.now().plus(startDays, ChronoUnit.DAYS)));
        promotion.setValidityEnd(Date.from(Instant.now().plus(endDays, ChronoUnit.DAYS)));
        return promotion;
    }
}
