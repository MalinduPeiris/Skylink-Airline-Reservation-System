package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.FlightDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {

	@Autowired
	private FlightRepository flightRepository;

	public List<FlightDTO> searchFlights(Date departureDate, String destination, Double maxPrice, String cabinClass) {
		List<Flight> flights = flightRepository.findAll();
		return flights.stream()
				.filter(f -> (departureDate == null || f.getDepartureDate().equals(departureDate))
						&& (destination == null || f.getDestination().equals(destination))
						&& (maxPrice == null || f.getPrice() <= maxPrice)
						&& (cabinClass == null || f.getCabinClass().name().equals(cabinClass)))
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	public List<FlightDTO> getAllFlights() {
		return flightRepository.findAll().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	private FlightDTO convertToDTO(Flight flight) {
		FlightDTO dto = new FlightDTO();
		dto.setId(flight.getId());
		dto.setFlightNumber(flight.getFlightNumber());
		dto.setDepartureDate(flight.getDepartureDate());
		dto.setArrivalDate(flight.getArrivalDate());
		dto.setOrigin(flight.getOrigin());
		dto.setDestination(flight.getDestination());
		dto.setPrice(flight.getPrice());
		dto.setCabinClass(flight.getCabinClass().name());
		dto.setSeatsAvailable(flight.getSeatsAvailable());
		return dto;
	}
}