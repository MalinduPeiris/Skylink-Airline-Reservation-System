package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FlightDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {

	private static final Logger logger = LoggerFactory.getLogger(FlightService.class);

	@Autowired
	private FlightRepository flightRepository;

	public List<String> getAllOrigins() {
		List<String> origins = flightRepository.findAllOrigins();
		logger.info("Retrieved {} origins from database: {}", origins.size(), origins);
		return origins;
	}

	public List<String> getAllDestinations() {
		List<String> destinations = flightRepository.findAllDestinations();
		logger.info("Retrieved {} destinations from database: {}", destinations.size(), destinations);
		return destinations;
	}

	@Transactional
	public FlightDTO createFlight(FlightDTO flightDTO) {
		// Validate required fields
		if (flightDTO.getFlightNumber() == null || flightDTO.getFlightNumber().trim().isEmpty()) {
			throw new IllegalArgumentException("Flight number is required");
		}
		if (flightDTO.getDepartureDate() == null) {
			throw new IllegalArgumentException("Departure date is required");
		}
		if (flightDTO.getArrivalDate() == null) {
			throw new IllegalArgumentException("Arrival date is required");
		}
		if (flightDTO.getOrigin() == null || flightDTO.getOrigin().trim().isEmpty()) {
			throw new IllegalArgumentException("Origin is required");
		}
		if (flightDTO.getDestination() == null || flightDTO.getDestination().trim().isEmpty()) {
			throw new IllegalArgumentException("Destination is required");
		}
		if (flightDTO.getPrice() == null || flightDTO.getPrice() <= 0) {
			throw new IllegalArgumentException("Valid price is required (must be greater than 0)");
		}
		if (flightDTO.getCabinClass() == null || flightDTO.getCabinClass().trim().isEmpty()) {
			throw new IllegalArgumentException("Cabin class is required");
		}
		if (flightDTO.getSeatsAvailable() == null || flightDTO.getSeatsAvailable() < 0) {
			throw new IllegalArgumentException("Valid seats available is required (must be 0 or greater)");
		}
		if (flightDTO.getAircraftType() == null || flightDTO.getAircraftType().trim().isEmpty()) {
			throw new IllegalArgumentException("Aircraft type is required");
		}

		// Validate dates
		if (flightDTO.getDepartureDate().after(flightDTO.getArrivalDate())) {
			throw new IllegalArgumentException("Departure date must be before arrival date");
		}

		// Validate cabin class
		try {
			Flight.CabinClass.valueOf(flightDTO.getCabinClass());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid cabin class: " + flightDTO.getCabinClass());
		}

		// Validate origin and destination are different
		if (flightDTO.getOrigin().equalsIgnoreCase(flightDTO.getDestination())) {
			throw new IllegalArgumentException("Origin and destination cannot be the same");
		}

		Flight flight = new Flight();
		flight.setFlightNumber(flightDTO.getFlightNumber().trim().toUpperCase());
		flight.setAircraftType(flightDTO.getAircraftType().trim());
		flight.setOrigin(flightDTO.getOrigin().trim().toUpperCase());
		flight.setDestination(flightDTO.getDestination().trim().toUpperCase());
		flight.setDepartureDate(flightDTO.getDepartureDate());
		flight.setArrivalDate(flightDTO.getArrivalDate());
		flight.setPrice(flightDTO.getPrice());
		flight.setCabinClass(Flight.CabinClass.valueOf(flightDTO.getCabinClass()));
		flight.setSeatsAvailable(flightDTO.getSeatsAvailable());
		flight.setStatus(flightDTO.getStatus() != null ? flightDTO.getStatus().trim() : "Scheduled");

		Flight savedFlight = flightRepository.save(flight);
		logger.info("Flight created: {}", savedFlight.getFlightNumber());
		return convertToDTO(savedFlight);
	}

	@Transactional
	public FlightDTO updateFlight(Long id, FlightDTO flightDTO) {
		// Validate ID
		if (id == null) {
			throw new IllegalArgumentException("Flight ID is required");
		}

		Flight flight = flightRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));

		// Validate and update fields if provided
		if (flightDTO.getFlightNumber() != null && !flightDTO.getFlightNumber().trim().isEmpty()) {
			flight.setFlightNumber(flightDTO.getFlightNumber().trim().toUpperCase());
		}
		
		if (flightDTO.getAircraftType() != null && !flightDTO.getAircraftType().trim().isEmpty()) {
			flight.setAircraftType(flightDTO.getAircraftType().trim());
		}
		
		if (flightDTO.getOrigin() != null && !flightDTO.getOrigin().trim().isEmpty()) {
			String origin = flightDTO.getOrigin().trim().toUpperCase();
			// Validate origin and destination are different if both are being updated
			String destination = flight.getDestination();
			if (flightDTO.getDestination() != null && !flightDTO.getDestination().trim().isEmpty()) {
				destination = flightDTO.getDestination().trim().toUpperCase();
			}
			if (origin.equalsIgnoreCase(destination)) {
				throw new IllegalArgumentException("Origin and destination cannot be the same");
			}
			flight.setOrigin(origin);
		}
		
		if (flightDTO.getDestination() != null && !flightDTO.getDestination().trim().isEmpty()) {
			String destination = flightDTO.getDestination().trim().toUpperCase();
			// Validate origin and destination are different if both are being updated
			String origin = flight.getOrigin();
			if (flightDTO.getOrigin() != null && !flightDTO.getOrigin().trim().isEmpty()) {
				origin = flightDTO.getOrigin().trim().toUpperCase();
			}
			if (origin.equalsIgnoreCase(destination)) {
				throw new IllegalArgumentException("Origin and destination cannot be the same");
			}
			flight.setDestination(destination);
		}
		
		if (flightDTO.getDepartureDate() != null) {
			flight.setDepartureDate(flightDTO.getDepartureDate());
			// Validate dates if both are provided
			if (flightDTO.getArrivalDate() != null && flightDTO.getDepartureDate().after(flightDTO.getArrivalDate())) {
				throw new IllegalArgumentException("Departure date must be before arrival date");
			} else if (flightDTO.getArrivalDate() == null && flight.getArrivalDate() != null && 
					flightDTO.getDepartureDate().after(flight.getArrivalDate())) {
				throw new IllegalArgumentException("Departure date must be before arrival date");
			}
		}
		
		if (flightDTO.getArrivalDate() != null) {
			flight.setArrivalDate(flightDTO.getArrivalDate());
			// Validate dates if both are provided
			if (flightDTO.getDepartureDate() != null && flightDTO.getDepartureDate().after(flightDTO.getArrivalDate())) {
				throw new IllegalArgumentException("Departure date must be before arrival date");
			} else if (flightDTO.getDepartureDate() == null && flight.getDepartureDate() != null && 
					flight.getDepartureDate().after(flightDTO.getArrivalDate())) {
				throw new IllegalArgumentException("Departure date must be before arrival date");
			}
		}
		
		if (flightDTO.getPrice() != null) {
			if (flightDTO.getPrice() <= 0) {
				throw new IllegalArgumentException("Valid price is required (must be greater than 0)");
			}
			flight.setPrice(flightDTO.getPrice());
		}
		
		if (flightDTO.getCabinClass() != null && !flightDTO.getCabinClass().trim().isEmpty()) {
			try {
				flight.setCabinClass(Flight.CabinClass.valueOf(flightDTO.getCabinClass()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid cabin class: " + flightDTO.getCabinClass());
			}
		}
		
		if (flightDTO.getSeatsAvailable() != null) {
			if (flightDTO.getSeatsAvailable() < 0) {
				throw new IllegalArgumentException("Valid seats available is required (must be 0 or greater)");
			}
			flight.setSeatsAvailable(flightDTO.getSeatsAvailable());
		}
		
		if (flightDTO.getStatus() != null && !flightDTO.getStatus().trim().isEmpty()) {
			flight.setStatus(flightDTO.getStatus().trim());
		}

		Flight updatedFlight = flightRepository.save(flight);
		logger.info("Flight updated: {}", updatedFlight.getFlightNumber());
		return convertToDTO(updatedFlight);
	}

	public FlightDTO getFlightById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Flight ID is required");
		}
		
		Flight flight = flightRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));
		return convertToDTO(flight);
	}

	public List<FlightDTO> getAllFlights() {
		List<Flight> flights = flightRepository.findAll();
		logger.info("Retrieved {} flights from database", flights.size());
		for (Flight flight : flights) {
			logger.info("Flight: ID={}, Number={}, Origin={}, Destination={}, Departure={}, Price={}, Class={}", 
				flight.getId(), flight.getFlightNumber(), flight.getOrigin(), flight.getDestination(),
				flight.getDepartureDate(), flight.getPrice(), flight.getCabinClass());
		}
		return flights.stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	@Transactional
	public void deleteFlight(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Flight ID is required");
		}
		
		Flight flight = flightRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));
		flightRepository.delete(flight);
		logger.info("Flight deleted: {}", flight.getFlightNumber());
	}

	// Updated method to search flights with all parameters including origin
	public List<FlightDTO> searchFlightsWithOrigin(Date departureDate, String origin, String destination, Double maxPrice, String cabinClass) {
		try {
			logger.info("=== SEARCH FLIGHTS METHOD STARTED ===");
			logger.info("Searching flights with parameters - departureDate: {}, origin: '{}', destination: '{}', maxPrice: {}, cabinClass: '{}'", 
				departureDate, origin, destination, maxPrice, cabinClass);

			// Try multiple search approaches
			List<Flight> flights = new ArrayList<>();
			
			// Approach 1: Native query search
			try {
				logger.info("Trying native query search...");
				flights = flightRepository.searchFlights(departureDate, origin, destination, maxPrice, cabinClass);
				logger.info("Native query search returned {} flights", flights.size());
			} catch (Exception e1) {
				logger.error("Native query search failed", e1);
				
				// Approach 2: Manual filtering as fallback
				try {
					logger.info("Falling back to manual filtering...");
					List<Flight> allFlights = flightRepository.findAll();
					logger.info("Total flights in database: {}", allFlights.size());
					
					flights = allFlights.stream()
							.filter(flight -> {
								// Filter by origin
								if (origin != null && !origin.isEmpty() && !flight.getOrigin().equalsIgnoreCase(origin)) {
									return false;
								}
								// Filter by destination
								if (destination != null && !destination.isEmpty() && !flight.getDestination().equalsIgnoreCase(destination)) {
									return false;
								}
								// Filter by departure date
								if (departureDate != null) {
									// Compare dates without time
									Date flightDate = flight.getDepartureDate();
									if (flightDate == null) return false;

									String flightDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(flightDate);
									String searchDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(departureDate);

									if (!flightDateStr.equals(searchDateStr)) {
										return false;
									}
								}
								// Filter by cabin class
								if (cabinClass != null && !cabinClass.isEmpty() && !flight.getCabinClass().name().equals(cabinClass)) {
									return false;
								}
								// Filter by max price
								if (maxPrice != null && flight.getPrice() > maxPrice) {
									return false;
								}
								return true;
							})
							.collect(Collectors.toList());
					logger.info("Manual filtering returned {} flights", flights.size());
				} catch (Exception e2) {
					logger.error("Manual filtering also failed", e2);
					flights = new ArrayList<>();
				}
			}
			
			// Log the flights found
			if (flights.isEmpty()) {
				logger.info("No flights found in search, logging database contents for debugging");
				// Let's also try getting all flights to see what we have
				List<Flight> allFlights = flightRepository.findAll();
				logger.info("Total flights in database: {}", allFlights.size());
				for (Flight flight : allFlights) {
					logger.info("Database flight: ID={}, Number={}, Origin='{}', Destination='{}', Departure={}, Price={}, Class={}", 
						flight.getId(), flight.getFlightNumber(), flight.getOrigin(), flight.getDestination(),
						flight.getDepartureDate(), flight.getPrice(), flight.getCabinClass());
				}
			} else {
				for (Flight flight : flights) {
					logger.info("Found flight: {} from {} to {} on {}", 
						flight.getFlightNumber(), flight.getOrigin(), flight.getDestination(), flight.getDepartureDate());
				}
			}

			List<FlightDTO> result = flights.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
			logger.info("=== SEARCH FLIGHTS METHOD COMPLETED: {} results ===", result.size());
			return result;
		} catch (Exception e) {
			logger.error("Error searching flights", e);
			return new ArrayList<>();
		}
	}

	private FlightDTO convertToDTO(Flight flight) {
		FlightDTO dto = new FlightDTO();
		dto.setId(flight.getId());
		dto.setFlightNumber(flight.getFlightNumber());
		dto.setAircraftType(flight.getAircraftType());
		dto.setOrigin(flight.getOrigin());
		dto.setDestination(flight.getDestination());
		dto.setDepartureDate(flight.getDepartureDate());
		dto.setArrivalDate(flight.getArrivalDate());
		dto.setPrice(flight.getPrice());
		dto.setCabinClass(flight.getCabinClass().name());
		dto.setSeatsAvailable(flight.getSeatsAvailable());
		dto.setStatus(flight.getStatus());
		return dto;
	}
	
	// Method to initialize sample flights for testing
	public void initializeSampleFlights() {
		try {
			// Check if there are already flights in the database
			if (flightRepository.count() == 0) {
				logger.info("No flights found in database, initializing sample flights");
				
				// Create sample flights
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				
				createSampleFlight("SL101", "Colombo", "London", "Boeing 787", 
					sdf.parse("2025-10-25 14:30"), 
					sdf.parse("2025-10-25 02:45"), 
					150000.0, "ECONOMY", 25, "Scheduled");
				
				createSampleFlight("SL205", "Los Angeles", "Sydney", "Airbus A380", 
					sdf.parse("2025-10-26 18:00"), 
					sdf.parse("2025-10-26 10:15"), 
					280000.0, "BUSINESS", 12, "Scheduled");
				
				createSampleFlight("SL312", "Dubai", "Colombo", "Boeing 777", 
					sdf.parse("2025-10-27 22:30"), 
					sdf.parse("2025-10-27 03:20"), 
					95000.0, "ECONOMY", 45, "Scheduled");
				
				createSampleFlight("SL401", "London", "Paris", "Boeing 787", 
					sdf.parse("2025-10-28 09:00"), 
					sdf.parse("2025-10-28 11:30"), 
					420000.0, "FIRST_CLASS", 8, "Scheduled");
				
				logger.info("Sample flights initialized successfully");
			} else {
				logger.info("Flights already exist in database, skipping initialization");
			}
		} catch (Exception e) {
			logger.error("Error initializing sample flights", e);
		}
	}
	
	// Helper method to create a sample flight
	private void createSampleFlight(String flightNumber, String origin, String destination, 
			String aircraftType, Date departureDate, Date arrivalDate, 
			Double price, String cabinClass, Integer seatsAvailable, String status) {
		try {
			FlightDTO flightDTO = new FlightDTO();
			flightDTO.setFlightNumber(flightNumber);
			flightDTO.setOrigin(origin);
			flightDTO.setDestination(destination);
			flightDTO.setAircraftType(aircraftType);
			flightDTO.setDepartureDate(departureDate);
			flightDTO.setArrivalDate(arrivalDate);
			flightDTO.setPrice(price);
			flightDTO.setCabinClass(cabinClass);
			flightDTO.setSeatsAvailable(seatsAvailable);
			flightDTO.setStatus(status);
			
			createFlight(flightDTO);
			logger.info("Created sample flight: {} from {} to {}", flightNumber, origin, destination);
		} catch (Exception e) {
			logger.error("Error creating sample flight: {}", flightNumber, e);
		}
	}
	
	// Method to test database connectivity and queries
	public void testDatabaseConnectivity() {
		try {
			logger.info("=== DATABASE CONNECTIVITY TEST ===");
			
			// Test 1: Count all flights
			long flightCount = flightRepository.count();
			logger.info("Total flights in database: {}", flightCount);
			
			// Test 2: Get all flights
			List<Flight> allFlights = flightRepository.findAll();
			logger.info("findAll() returned {} flights", allFlights.size());
			
			// Test 3: Get distinct origins
			List<String> origins = flightRepository.findAllOrigins();
			logger.info("findAllOrigins() returned {} origins: {}", origins.size(), origins);
			
			// Test 4: Get distinct destinations
			List<String> destinations = flightRepository.findAllDestinations();
			logger.info("findAllDestinations() returned {} destinations: {}", destinations.size(), destinations);
			
			// Test 5: Try a simple search with no parameters
			List<Flight> searchResults = flightRepository.searchFlights(null, null, null, null, null);
			logger.info("searchFlights(null, null, null, null, null) returned {} flights", searchResults.size());
			
			logger.info("=== DATABASE CONNECTIVITY TEST COMPLETED ===");
		} catch (Exception e) {
			logger.error("Database connectivity test failed", e);
		}
	}
	
	// Method to verify database data
	public void verifyDatabaseData() {
		try {
			logger.info("=== DATABASE DATA VERIFICATION ===");
			
			// Get all flights and log their details
			List<Flight> allFlights = flightRepository.findAll();
			logger.info("Total flights in database: {}", allFlights.size());
			
			for (Flight flight : allFlights) {
				logger.info("Flight ID: {}, Number: {}, Origin: '{}', Destination: '{}', Departure: {}, Cabin: {}, Price: {}", 
					flight.getId(), flight.getFlightNumber(), flight.getOrigin(), flight.getDestination(),
					flight.getDepartureDate(), flight.getCabinClass(), flight.getPrice());
			}
			
			// Test a specific search that should work
			if (!allFlights.isEmpty()) {
				Flight sampleFlight = allFlights.get(0);
				logger.info("Testing search for flight from '{}' to '{}' on date {}", 
					sampleFlight.getOrigin(), sampleFlight.getDestination(), sampleFlight.getDepartureDate());
				
				// Try searching for this specific flight
				List<Flight> searchResults = flightRepository.searchFlights(
					sampleFlight.getDepartureDate(), 
					sampleFlight.getOrigin(), 
					sampleFlight.getDestination(), 
					sampleFlight.getPrice(), 
					sampleFlight.getCabinClass().name()
				);
				logger.info("Search returned {} results", searchResults.size());
				
				for (Flight result : searchResults) {
					logger.info("Search result: ID: {}, Number: {}, Origin: '{}', Destination: '{}'", 
						result.getId(), result.getFlightNumber(), result.getOrigin(), result.getDestination());
				}
			}
			
			logger.info("=== DATABASE DATA VERIFICATION COMPLETED ===");
		} catch (Exception e) {
			logger.error("Database data verification failed", e);
		}
	}
}