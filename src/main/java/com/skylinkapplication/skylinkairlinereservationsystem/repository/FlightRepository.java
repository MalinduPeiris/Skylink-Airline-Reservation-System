package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {
	// Updated custom JPQL query to include origin parameter for efficient, dynamic searching
	@Query(value = "SELECT * FROM flight f WHERE " +
			"(:departureDate IS NULL OR DATE(f.departure_date) = DATE(:departureDate)) AND " +
			"(:origin IS NULL OR f.origin = :origin) AND " +
			"(:destination IS NULL OR f.destination = :destination) AND " +
			"(:maxPrice IS NULL OR f.price <= :maxPrice) AND " +
			"(:cabinClass IS NULL OR f.cabin_class = :cabinClass)", nativeQuery = true)
	List<Flight> searchFlights(
			@Param("departureDate") Date departureDate,
			@Param("origin") String origin,
			@Param("destination") String destination,
			@Param("maxPrice") Double maxPrice,
			@Param("cabinClass") String cabinClass
	);

	@Query("SELECT DISTINCT f.origin FROM Flight f")
	List<String> findAllOrigins();

	@Query("SELECT DISTINCT f.destination FROM Flight f")
	List<String> findAllDestinations();

	List<Flight> findByPriceLessThanEqual(Double price);
	List<Flight> findByCabinClass(Flight.CabinClass cabinClass);
	List<Flight> findBySeatsAvailableGreaterThan(Integer seats);
	List<Flight> findByOriginAndDestination(String origin, String destination);
}