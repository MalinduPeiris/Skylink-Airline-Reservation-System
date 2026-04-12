package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {
	List<Flight> findByDepartureDateAndDestination(Date departureDate, String destination);
	List<Flight> findByPriceLessThanEqual(Double price);
	List<Flight> findByCabinClass(Flight.CabinClass cabinClass);
	List<Flight> findBySeatsAvailableGreaterThan(Integer seats);
}