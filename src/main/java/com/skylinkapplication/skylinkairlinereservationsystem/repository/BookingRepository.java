package com.skylinkapplication.skylinkairlinereservationsystem.repository;

import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
	List<Booking> findByUser_Id(Long userId);
	List<Booking> findByStatus(Booking.Status status);
	List<Booking> findByFlight_Id(Long flightId);
}