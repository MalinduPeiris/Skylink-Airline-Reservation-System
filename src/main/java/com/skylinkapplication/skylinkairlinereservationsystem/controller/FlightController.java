package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FlightDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.service.FlightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/api/flight")
public class FlightController {

    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);

    @Autowired
    private FlightService flightService;

    @GetMapping("/search")
    public String searchFlights(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date departureDate,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String cabinClass,
            @RequestParam(required = false) Integer passengers,
            Model model) {
        try {
            logger.info("=== FLIGHT SEARCH REQUEST RECEIVED ===");
            logger.info("Parameters received:");
            logger.info("  departureDate: {}", departureDate);
            logger.info("  origin: '{}'", origin);
            logger.info("  destination: '{}'", destination);
            logger.info("  maxPrice: {}", maxPrice);
            logger.info("  cabinClass: '{}'", cabinClass);
            logger.info("  passengers: {}", passengers);

            // Test database connectivity
            flightService.testDatabaseConnectivity();
            
            // Verify database data
            flightService.verifyDatabaseData();

            // Initialize sample flights if database is empty
            flightService.initializeSampleFlights();

            // Get all unique origins and destinations for the search form
            List<String> origins = flightService.getAllOrigins();
            List<String> destinations = flightService.getAllDestinations();
            logger.info("Retrieved {} origins and {} destinations for search form", origins.size(), destinations.size());
            model.addAttribute("origins", origins);
            model.addAttribute("destinations", destinations);

            // Search flights
            logger.info("Calling flightService.searchFlightsWithOrigin...");
            List<FlightDTO> filteredFlights = flightService.searchFlightsWithOrigin(
                    departureDate, origin, destination, maxPrice, cabinClass);
            logger.info("Flight service returned {} flights", filteredFlights.size());

            // Apply passenger filtering
            if (passengers != null && passengers > 0) {
                logger.info("Applying passenger filter: {}+", passengers);
                int beforeFilter = filteredFlights.size();
                filteredFlights = filteredFlights.stream()
                        .filter(flight -> flight.getSeatsAvailable() >= passengers)
                        .collect(java.util.stream.Collectors.toList());
                logger.info("Passenger filter: {} -> {} flights", beforeFilter, filteredFlights.size());
            }

            model.addAttribute("flights", filteredFlights);
            model.addAttribute("searchResults", filteredFlights);
            model.addAttribute("searchPerformed", true);
            model.addAttribute("resultCount", filteredFlights.size());

            // Keep search parameters in model for form
            model.addAttribute("searchOrigin", origin);
            model.addAttribute("searchDestination", destination);
            model.addAttribute("searchDepartureDate", departureDate);
            model.addAttribute("searchCabinClass", cabinClass);
            model.addAttribute("searchPassengers", passengers);

            logger.info("=== FLIGHT SEARCH COMPLETED: {} flights found ===", filteredFlights.size());

            return "flight-search-results";
        } catch (Exception e) {
            logger.error("Error searching flights", e);
            model.addAttribute("error", "Unable to search flights. Please try again.");
            model.addAttribute("searchResults", java.util.Collections.emptyList());
            model.addAttribute("resultCount", 0);
            return "flight-search-results";
        }
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasRole('IT_SYSTEM_ENGINEER')")
    public String viewSchedule(Model model) {
        try {
            List<FlightDTO> flights = flightService.getAllFlights();
            model.addAttribute("flights", flights);
            logger.info("Flight schedule retrieved: {} flights found", flights.size());
            return "flight-admin-schedule-management";
        } catch (RuntimeException e) {
            logger.error("Error retrieving flight schedule", e);
            model.addAttribute("error", "Unable to load flight schedule. Please try again.");
            return "error";
        }
    }

    @GetMapping("/all")
    public String viewAllFlights(Model model) {
        try {
            List<FlightDTO> flights = flightService.getAllFlights();
            model.addAttribute("allFlights", flights);
            logger.info("All flights page loaded: {} flights", flights.size());
            return "flights";
        } catch (Exception e) {
            logger.error("Error loading all flights", e);
            model.addAttribute("error", "Unable to load flights");
            return "flights";
        }
    }
}