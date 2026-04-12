package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FlightDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.service.AuthService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FlightService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

	@Autowired
	private FlightService flightService;

	@Autowired
	private PromotionService promotionService;

	@Autowired
	private AuthService authService;

	@GetMapping({"/", "/index", "/index.html"})
	public String home(Model model, HttpSession session) {
		logger.info("Serving home page");

		try {
			// Test database connectivity
			flightService.testDatabaseConnectivity();
			
			// Verify database data
			flightService.verifyDatabaseData();

			// Initialize sample flights if database is empty
			flightService.initializeSampleFlights();

			// Load top 3 currently available flights
			List<FlightDTO> topFlights = flightService.getAllFlights().stream()
					.filter(f -> f.getSeatsAvailable() != null && f.getSeatsAvailable() > 0)
					.sorted(Comparator.comparing(FlightDTO::getDepartureDate))
					.limit(3)
					.collect(Collectors.toList());
			model.addAttribute("topFlights", topFlights);

			// Load top 4 active promotions
			List<PromotionDTO> topPromotions = promotionService.getAllPromotions().stream()
					.filter(promo -> {
						Date now = new Date();
						return promo.getValidityStart().before(now) && promo.getValidityEnd().after(now);
					})
					.sorted(Comparator.comparing(PromotionDTO::getDiscount).reversed())
					.limit(4)
					.collect(Collectors.toList());
			model.addAttribute("topPromotions", topPromotions);

			// Get all origins and destinations for the search form
			List<String> origins = flightService.getAllOrigins();
			List<String> destinations = flightService.getAllDestinations();
			logger.info("Retrieved {} origins and {} destinations for home page", origins.size(), destinations.size());
			model.addAttribute("origins", origins);
			model.addAttribute("destinations", destinations);

			// Check for user-specific promotions if logged in
			Long userId = (Long) session.getAttribute("userId");
			if (userId != null) {
				try {
					User user = authService.getUserById(userId);
					List<PromotionDTO> userPromotions = topPromotions.stream()
							.filter(promo -> isSpecialForUser(promo, user))
							.limit(4)
							.collect(Collectors.toList());

					if (!userPromotions.isEmpty()) {
						model.addAttribute("hasSpecialOffers", true);
						model.addAttribute("specialPromotionsCount", userPromotions.size());
					}
				} catch (Exception e) {
					logger.warn("Could not check special promotions for user ID: {}", userId);
				}
			}

			logger.info("Home page loaded with {} flights and {} promotions", topFlights.size(), topPromotions.size());
		} catch (Exception e) {
			logger.error("Error loading home page data", e);
			// Set empty lists to avoid null pointer exceptions
			model.addAttribute("topFlights", List.of());
			model.addAttribute("topPromotions", List.of());
			model.addAttribute("origins", List.of());
			model.addAttribute("destinations", List.of());
		}

		return "index";
	}

	@GetMapping("/flights")
	public String flightsRedirect() {
		return "redirect:/api/flight/all";
	}

	@GetMapping("/booking")
	public String bookingShortcut() {
		return "booking";
	}

	@GetMapping("/traveler-payment")
	public String travelerPayment(@RequestParam(required = false, name = "bookingId") String bookingId,
								  @RequestParam(required = false, name = "subtotal") Double subtotal,
								  @RequestParam(required = false, name = "taxes") Double taxes,
								  @RequestParam(required = false, name = "total") Double total,
								  Model model) {
		model.addAttribute("bookingRef", bookingId != null ? bookingId : "N/A");
		model.addAttribute("subtotal", subtotal != null ? String.format("%.2f", subtotal) : "0.00");
		model.addAttribute("taxes", taxes != null ? String.format("%.2f", taxes) : "0.00");
		model.addAttribute("total", total != null ? String.format("%.2f", total) : "0.00");
		return "traveler-payment";
	}

	@GetMapping("/booking-confirmation")
	public String bookingConfirmation() {
		return "booking-confirmation";
	}

	@GetMapping("/my-bookings")
	public String myBookings() {
		return "redirect:/api/user/profile";
	}

	private boolean isSpecialForUser(PromotionDTO promo, User user) {
		String criteria = promo.getTargetCriteria().toLowerCase();
		String userRole = user.getRole().name().toLowerCase();

		if (criteria.contains("frequent") && userRole.contains("frequent_traveler")) {
			return true;
		}
		if (criteria.contains("premium") || criteria.contains("vip")) {
			return userRole.contains("frequent_traveler");
		}
		if (criteria.contains("business") || criteria.contains("first")) {
			return true;
		}

		return false;
	}
}