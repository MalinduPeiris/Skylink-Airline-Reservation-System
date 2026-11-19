package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PromotionRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionEligibilityManagementTest {
    @Mock private PromotionRepository promotions;
    @Mock private UserRepository users;
    @InjectMocks private PromotionService service;

    @Test
    void creationRequiresExplicitScopeAndBeginsInactiveWithoutChangingLegacyRows() {
        PromotionDTO request = new PromotionDTO();
        request.setPromoCode("NEW20");
        request.setTargetCriteria("descriptive");
        request.setDiscount(20.0);
        request.setValidityStart(new Date());
        request.setValidityEnd(new Date());
        assertThrows(IllegalArgumentException.class, () -> service.createPromotion(request));
        verify(promotions, never()).save(any());

        request.setScope(Promotion.Scope.GENERAL);
        when(promotions.save(any(Promotion.class))).thenAnswer(invocation -> {
            Promotion saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        var created = service.createPromotion(request);
        assertEquals(20L, created.getId());
        assertEquals(Promotion.Scope.GENERAL, created.getScope());
        assertFalse(created.getActive());
    }

    @Test
    void explicitTargetedClassificationKeepsPromotionIdAndLinksOnlySelectedTraveler() {
        Promotion legacy = new Promotion();
        legacy.setId(19L);
        User traveler = new User();
        traveler.setId(7L);
        traveler.setRole(Role.FREQUENT_TRAVELER);
        when(promotions.findById(19L)).thenReturn(Optional.of(legacy));
        when(users.findAllById(List.of(7L))).thenReturn(List.of(traveler));
        when(promotions.save(legacy)).thenReturn(legacy);

        var result = service.updateEligibility(19L, Promotion.Scope.TARGETED, true, List.of(7L, 7L));

        assertEquals(19L, result.getId());
        assertEquals(Promotion.Scope.TARGETED, result.getScope());
        assertEquals(List.of(7L), result.getEligibleUserIds());
        assertTrue(result.getActive());
    }

    @Test
    void activeTargetedWithoutMembershipAndUnknownUsersCannotBeSaved() {
        Promotion legacy = new Promotion();
        legacy.setId(19L);
        when(promotions.findById(19L)).thenReturn(Optional.of(legacy));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateEligibility(19L, Promotion.Scope.TARGETED, true, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateEligibility(19L, Promotion.Scope.TARGETED, true, List.of(999L)));
        verify(promotions, never()).save(any());
        assertNull(legacy.getScope());
    }

    @Test
    void generalPromotionCannotRetainTargetedUserSelection() {
        Promotion legacy = new Promotion();
        legacy.setId(19L);
        when(promotions.findById(19L)).thenReturn(Optional.of(legacy));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateEligibility(19L, Promotion.Scope.GENERAL, true, List.of(7L)));
        verify(promotions, never()).save(any());
    }
}
