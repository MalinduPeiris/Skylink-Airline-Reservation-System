package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
	List<Promotion> findByValidityStartBeforeAndValidityEndAfter(Date start, Date end);
	List<Promotion> findByTargetCriteriaContaining(String targetCriteria);
}