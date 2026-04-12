package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

	@Autowired
	private BookingRepository bookingRepository;

	public List<BookingDTO> getBookingsByUserId(Long userId) {
		return bookingRepository.findByUser_Id(userId).stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	public BookingDTO createBooking(BookingDTO bookingDTO) {
		Booking booking = new Booking();
		booking.setBookingDate(new Date());
		booking.setStatus(Booking.Status.PENDING);
		booking.setPassengers(bookingDTO.getPassengers());
		// Set user and flight (to be implemented with proper mapping)
		Booking savedBooking = bookingRepository.save(booking);
		return convertToDTO(savedBooking);
	}

	public void cancelBooking(Long bookingId) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found"));
		booking.setStatus(Booking.Status.CANCELLED);
		bookingRepository.save(booking);
	}

	private BookingDTO convertToDTO(Booking booking) {
		BookingDTO dto = new BookingDTO();
		dto.setId(booking.getId());
		dto.setBookingDate(booking.getBookingDate());
		dto.setStatus(booking.getStatus().name());
		dto.setPassengers(booking.getPassengers());
		dto.setFlightId(booking.getFlight() != null ? booking.getFlight().getId() : null);
		dto.setPaymentId(booking.getPayment() != null ? booking.getPayment().getId() : null);
		return dto;
	}
}