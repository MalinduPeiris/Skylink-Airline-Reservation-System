package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Promotion;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Role;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PromotionRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class PromotionService {

	private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);

	@Autowired
	private PromotionRepository promotionRepository;
	@Autowired
	private UserRepository userRepository;
	private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

	@Transactional
	public PromotionDTO createPromotion(PromotionDTO promotionDTO) {
		logger.info("Creating promotion with data: targetCriteria={}, discount={}, validityStart={}, validityEnd={}, promoCode={}",
				promotionDTO.getTargetCriteria(), promotionDTO.getDiscount(),
				promotionDTO.getValidityStart(), promotionDTO.getValidityEnd(), promotionDTO.getPromoCode());


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
		if (localDate(promotionDTO.getValidityStart()).isAfter(localDate(promotionDTO.getValidityEnd()))) {
			throw new IllegalArgumentException("Validity start date must be before validity end date");
		}

		if (promotionDTO.getDiscount() > 100) {
			throw new IllegalArgumentException("Discount cannot be greater than 100%");
		}

		Promotion promotion = new Promotion();
		promotion.setDiscount(promotionDTO.getDiscount());
		promotion.setValidityStart(promotionDTO.getValidityStart());
		promotion.setValidityEnd(promotionDTO.getValidityEnd());
		promotion.setTargetCriteria(promotionDTO.getTargetCriteria().trim());
		promotion.setPromoCode(promotionDTO.getPromoCode().toUpperCase().trim());
		if (promotionDTO.getScope() == null) {
			throw new IllegalArgumentException("Explicit promotion scope is required");
		}
		promotion.setScope(promotionDTO.getScope());
		promotion.setActive(false); // Staff must explicitly activate after reviewing eligibility.

		Promotion savedPromotion = promotionRepository.save(promotion);
		logger.info("Promotion created successfully with ID: {}", savedPromotion.getId());
		return convertToDTO(savedPromotion);
	}

	@Transactional
	public PromotionDTO updatePromotion(Long id, PromotionDTO updatedPromotionDTO) {
		logger.info("Updating promotion ID {} with data: targetCriteria={}, discount={}, validityStart={}, validityEnd={}, promoCode={}",
				id, updatedPromotionDTO.getTargetCriteria(), updatedPromotionDTO.getDiscount(),
				updatedPromotionDTO.getValidityStart(), updatedPromotionDTO.getValidityEnd(), updatedPromotionDTO.getPromoCode());

		if (id == null) {
			throw new IllegalArgumentException("Promotion ID is required");
		}

		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));

		if (updatedPromotionDTO.getDiscount() != null) {
			if (updatedPromotionDTO.getDiscount() <= 0) {
				throw new IllegalArgumentException("Valid discount is required (must be greater than 0)");
			}
			if (updatedPromotionDTO.getDiscount() > 100) {
				throw new IllegalArgumentException("Discount cannot be greater than 100%");
			}
			promotion.setDiscount(updatedPromotionDTO.getDiscount());
		}
		
		Date start = updatedPromotionDTO.getValidityStart() != null ? updatedPromotionDTO.getValidityStart() : promotion.getValidityStart();
		Date end = updatedPromotionDTO.getValidityEnd() != null ? updatedPromotionDTO.getValidityEnd() : promotion.getValidityEnd();
		if (localDate(start).isAfter(localDate(end))) {
			throw new IllegalArgumentException("Validity start date must be before validity end date");
		}
		promotion.setValidityStart(start);
		promotion.setValidityEnd(end);
		
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

	@Transactional(readOnly = true)
	public PromotionDTO getPromotionById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Promotion ID is required");
		}
		
		logger.info("Retrieving promotion by ID: {}", id);
		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Promotion not found"));
		return convertToDTO(promotion);
	}

	@Transactional(readOnly = true)
	public List<PromotionDTO> getAllPromotions() {
		logger.info("Retrieving all promotions");
		return promotionRepository.findAll().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public Optional<PromotionDTO> findRedeemablePromotion(String code, User traveler) {
		if (code == null || code.isBlank() || traveler == null || traveler.getId() == null
				|| traveler.getRole() != Role.FREQUENT_TRAVELER) {
			return Optional.empty();
		}
		return promotionRepository.findByPromoCodeIgnoreCase(code.trim())
				.filter(p -> isCurrent(p, LocalDate.now(COLOMBO)))
				.filter(p -> p.getScope() == Promotion.Scope.GENERAL ||
						(p.getScope() == Promotion.Scope.TARGETED && isLinked(p, traveler)))
				.map(this::convertToDTO);
	}

	@Transactional(readOnly = true)
	public List<PromotionDTO> getVisiblePromotions(User traveler) {
		LocalDate today = LocalDate.now(COLOMBO);
		return promotionRepository.findAll().stream()
				.filter(p -> isCurrent(p, today))
				.filter(p -> p.getScope() == Promotion.Scope.GENERAL ||
						(p.getScope() == Promotion.Scope.TARGETED && traveler != null
								&& traveler.getId() != null && traveler.getRole() == Role.FREQUENT_TRAVELER && isLinked(p, traveler)))
				.map(this::convertToDTO).collect(Collectors.toList());
	}

	@Transactional
	public PromotionDTO updateEligibility(Long id, Promotion.Scope scope, boolean active, List<Long> userIds) {
		if (id == null || scope == null) throw new IllegalArgumentException("Promotion and explicit scope are required");
		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Promotion not found"));
		List<Long> uniqueIds = userIds == null ? List.of() : userIds.stream()
				.filter(java.util.Objects::nonNull).distinct().toList();
		if (scope == Promotion.Scope.GENERAL && !uniqueIds.isEmpty()) {
			throw new IllegalArgumentException("General promotions cannot have targeted users");
		}
		if (scope == Promotion.Scope.TARGETED && active && uniqueIds.isEmpty()) {
			throw new IllegalArgumentException("An active targeted promotion requires linked travelers");
		}
		List<User> users = new ArrayList<>();
		if (!uniqueIds.isEmpty()) {
			userRepository.findAllById(uniqueIds).forEach(users::add);
			if (users.size() != uniqueIds.size() || users.stream().anyMatch(u -> u.getRole() != Role.FREQUENT_TRAVELER)) {
				throw new IllegalArgumentException("Every linked user must be an existing traveler");
			}
		}
		promotion.setScope(scope);
		promotion.setActive(active);
		promotion.setUsers(users);
		return convertToDTO(promotionRepository.save(promotion));
	}

	private boolean isLinked(Promotion promotion, User traveler) {
		return promotion.getUsers() != null && promotion.getUsers().stream()
				.anyMatch(user -> traveler.getId().equals(user.getId()));
	}

	private boolean isCurrent(Promotion promotion, LocalDate today) {
		return Boolean.TRUE.equals(promotion.getActive()) && promotion.getScope() != null
				&& promotion.getValidityStart() != null && promotion.getValidityEnd() != null
				&& !today.isBefore(localDate(promotion.getValidityStart()))
				&& !today.isAfter(localDate(promotion.getValidityEnd()))
				&& promotion.getDiscount() != null && Double.isFinite(promotion.getDiscount())
				&& promotion.getDiscount() > 0 && promotion.getDiscount() <= 100;
	}

	private LocalDate localDate(Date date) { return date.toInstant().atZone(COLOMBO).toLocalDate(); }

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
		dto.setScope(promotion.getScope());
		dto.setActive(promotion.getActive());
		dto.setEligibleUserIds(promotion.getUsers() == null ? List.of() : promotion.getUsers().stream().map(User::getId).toList());
		return dto;
	}
}
