package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
	List<Booking> findByUser_Id(Long userId);
	List<Booking> findByStatus(Booking.Status status);
	List<Booking> findByFlight_Id(Long flightId);

	@Query("SELECT COALESCE(SUM(b.passengers), 0) FROM Booking b WHERE b.status = 'CONFIRMED'")
	long sumConfirmedPassengers();

	@Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CONFIRMED'")
	long countConfirmedBookings();

	@Query("SELECT COUNT(b) FROM Booking b WHERE b.status != 'CANCELLED'")
	long countNonCancelledBookings();
}