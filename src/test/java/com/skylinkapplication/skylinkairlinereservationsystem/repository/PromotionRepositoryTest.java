package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:promotion_repository_test;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PromotionRepositoryTest {
    @Autowired private TestEntityManager entities;
    @Autowired private PromotionRepository promotions;

    @Test
    void scopeAndLinkedUsersPersistWithoutChangingLegacyNullClassification() {
        User traveler = new User();
        traveler.setUsername("promo_traveler");
        traveler.setEmail("promo@example.test");
        traveler.setPassword("test-only");
        traveler.setRole(Role.FREQUENT_TRAVELER);
        entities.persistAndFlush(traveler);

        Promotion targeted = new Promotion();
        targeted.setPromoCode("TARGET20");
        targeted.setTargetCriteria("descriptive");
        targeted.setDiscount(20.0);
        targeted.setValidityStart(new Date());
        targeted.setValidityEnd(new Date());
        targeted.setScope(Promotion.Scope.TARGETED);
        targeted.setActive(true);
        targeted.setUsers(List.of(traveler));
        entities.persistAndFlush(targeted);

        Promotion legacy = new Promotion();
        legacy.setPromoCode("LEGACY20");
        legacy.setTargetCriteria("descriptive");
        legacy.setDiscount(20.0);
        legacy.setValidityStart(new Date());
        legacy.setValidityEnd(new Date());
        entities.persistAndFlush(legacy);
        entities.clear();

        Promotion reloaded = promotions.findByPromoCodeIgnoreCase("target20").orElseThrow();
        assertEquals(targeted.getId(), reloaded.getId());
        assertEquals(Promotion.Scope.TARGETED, reloaded.getScope());
        assertTrue(reloaded.getActive());
        assertEquals(List.of(traveler.getId()), reloaded.getUsers().stream().map(User::getId).toList());
        Promotion unclassified = promotions.findByPromoCodeIgnoreCase("legacy20").orElseThrow();
        assertNull(unclassified.getScope());
        assertNull(unclassified.getActive());
    }
}
