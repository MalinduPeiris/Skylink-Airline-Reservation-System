package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionService {

	private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);

	@Autowired
	private PromotionRepository promotionRepository;

	@Transactional
	public PromotionDTO createPromotion(PromotionDTO promotionDTO) {
		logger.info("Creating promotion with data: targetCriteria={}, discount={}, validityStart={}, validityEnd={}, promoCode={}",
				promotionDTO.getTargetCriteria(), promotionDTO.getDiscount(),
				promotionDTO.getValidityStart(), promotionDTO.getValidityEnd(), promotionDTO.getPromoCode());

		// Validate required fields
		if (promotionDTO.getDiscount() == null || promotionDTO.getDiscount() <= 0) {
			throw new IllegalArgumentException("Valid discount is required (must be greater than 0)");
		}
		if (promotionDTO.getValidityStart() == null) {
			throw new IllegalArgumentException("Validity start date is required");
		}
		if (promotionDTO.getValidityEnd() == null) {
			throw new IllegalArgumentException("Validity end date is required");
		}
		if (promotionDTO.getTargetCriteria() == null || promotionDTO.getTargetCriteria().trim().isEmpty()) {
			throw new IllegalArgumentException("Target criteria is required");
		}
		if (promotionDTO.getPromoCode() == null || promotionDTO.getPromoCode().trim().isEmpty()) {
			throw new IllegalArgumentException("Promo code is required");
		}

		// Validate dates
		if (!promotionDTO.getValidityStart().before(promotionDTO.getValidityEnd())) {
			throw new IllegalArgumentException("Validity start date must be before validity end date");
		}

		// Validate discount range (0-100%)
		if (promotionDTO.getDiscount() > 100) {
			throw new IllegalArgumentException("Discount cannot be greater than 100%");
		}

		Promotion promotion = new Promotion();
		promotion.setDiscount(promotionDTO.getDiscount());
		promotion.setValidityStart(promotionDTO.getValidityStart());
		promotion.setValidityEnd(promotionDTO.getValidityEnd());
		promotion.setTargetCriteria(promotionDTO.getTargetCriteria().trim());
		promotion.setPromoCode(promotionDTO.getPromoCode().toUpperCase().trim());

		Promotion savedPromotion = promotionRepository.save(promotion);
		logger.info("Promotion created successfully with ID: {}", savedPromotion.getId());
		return convertToDTO(savedPromotion);
	}

	@Transactional
	public PromotionDTO updatePromotion(Long id, PromotionDTO updatedPromotionDTO) {
		logger.info("Updating promotion ID {} with data: targetCriteria={}, discount={}, validityStart={}, validityEnd={}, promoCode={}",
				id, updatedPromotionDTO.getTargetCriteria(), updatedPromotionDTO.getDiscount(),
				updatedPromotionDTO.getValidityStart(), updatedPromotionDTO.getValidityEnd(), updatedPromotionDTO.getPromoCode());

		// Validate ID
		if (id == null) {
			throw new IllegalArgumentException("Promotion ID is required");
		}

		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));

		// Validate and update fields if provided
		if (updatedPromotionDTO.getDiscount() != null) {
			if (updatedPromotionDTO.getDiscount() <= 0) {
				throw new IllegalArgumentException("Valid discount is required (must be greater than 0)");
			}
			if (updatedPromotionDTO.getDiscount() > 100) {
				throw new IllegalArgumentException("Discount cannot be greater than 100%");
			}
			promotion.setDiscount(updatedPromotionDTO.getDiscount());
		}
		
		if (updatedPromotionDTO.getValidityStart() != null) {
			promotion.setValidityStart(updatedPromotionDTO.getValidityStart());
			// Validate dates if both are provided
			if (updatedPromotionDTO.getValidityEnd() != null && 
				!updatedPromotionDTO.getValidityStart().before(updatedPromotionDTO.getValidityEnd())) {
				throw new IllegalArgumentException("Validity start date must be before validity end date");
			} else if (updatedPromotionDTO.getValidityEnd() == null && promotion.getValidityEnd() != null &&
				!updatedPromotionDTO.getValidityStart().before(promotion.getValidityEnd())) {
				throw new IllegalArgumentException("Validity start date must be before validity end date");
			}
		}
		
		if (updatedPromotionDTO.getValidityEnd() != null) {
			promotion.setValidityEnd(updatedPromotionDTO.getValidityEnd());
			// Validate dates if both are provided
			if (updatedPromotionDTO.getValidityStart() != null && 
				!updatedPromotionDTO.getValidityStart().before(updatedPromotionDTO.getValidityEnd())) {
				throw new IllegalArgumentException("Validity start date must be before validity end date");
			} else if (updatedPromotionDTO.getValidityStart() == null && promotion.getValidityStart() != null &&
				!promotion.getValidityStart().before(updatedPromotionDTO.getValidityEnd())) {
				throw new IllegalArgumentException("Validity start date must be before validity end date");
			}
		}
		
		if (updatedPromotionDTO.getTargetCriteria() != null && !updatedPromotionDTO.getTargetCriteria().trim().isEmpty()) {
			promotion.setTargetCriteria(updatedPromotionDTO.getTargetCriteria().trim());
		}
		
		if (updatedPromotionDTO.getPromoCode() != null && !updatedPromotionDTO.getPromoCode().trim().isEmpty()) {
			promotion.setPromoCode(updatedPromotionDTO.getPromoCode().toUpperCase().trim());
		}

		Promotion updatedPromotion = promotionRepository.save(promotion);
		logger.info("Promotion updated successfully with ID: {}", updatedPromotion.getId());
		return convertToDTO(updatedPromotion);
	}

	public PromotionDTO getPromotionById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Promotion ID is required");
		}
		
		logger.info("Retrieving promotion by ID: {}", id);
		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));
		return convertToDTO(promotion);
	}

	public List<PromotionDTO> getAllPromotions() {
		logger.info("Retrieving all promotions");
		return promotionRepository.findAll().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	@Transactional
	public void deletePromotion(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Promotion ID is required");
		}
		
		logger.info("Deleting promotion by ID: {}", id);
		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));
		promotionRepository.delete(promotion);
		logger.info("Promotion deleted successfully with ID: {}", id);
	}

	private PromotionDTO convertToDTO(Promotion promotion) {
		PromotionDTO dto = new PromotionDTO();
		dto.setId(promotion.getId());
		dto.setDiscount(promotion.getDiscount());
		dto.setValidityStart(promotion.getValidityStart());
		dto.setValidityEnd(promotion.getValidityEnd());
		dto.setTargetCriteria(promotion.getTargetCriteria());
		dto.setPromoCode(promotion.getPromoCode());
		return dto;
	}
}