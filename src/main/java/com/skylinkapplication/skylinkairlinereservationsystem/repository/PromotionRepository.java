package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
	@EntityGraph(attributePaths = "users")
	Optional<Promotion> findByPromoCodeIgnoreCase(String promoCode);
	List<Promotion> findByValidityStartBeforeAndValidityEndAfter(Date start, Date end);
	List<Promotion> findByTargetCriteriaContaining(String targetCriteria);
}
