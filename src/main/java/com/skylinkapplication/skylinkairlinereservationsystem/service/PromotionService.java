package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PromotionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionService {

	@Autowired
	private PromotionRepository promotionRepository;

	public List<PromotionDTO> getAllPromotions() {
		return promotionRepository.findAll().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	public void createPromotion(PromotionDTO promotionDTO) {
		Promotion promotion = new Promotion();
		promotion.setDiscount(promotionDTO.getDiscount());
		promotion.setValidityStart(promotionDTO.getValidityStart());
		promotion.setValidityEnd(promotionDTO.getValidityEnd());
		promotion.setTargetCriteria(promotionDTO.getTargetCriteria());
		// Set users (to be implemented with proper mapping)
		promotionRepository.save(promotion);
	}

	private PromotionDTO convertToDTO(Promotion promotion) {
		PromotionDTO dto = new PromotionDTO();
		dto.setId(promotion.getId());
		dto.setDiscount(promotion.getDiscount());
		dto.setValidityStart(promotion.getValidityStart());
		dto.setValidityEnd(promotion.getValidityEnd());
		dto.setTargetCriteria(promotion.getTargetCriteria());
		return dto;
	}
}