package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingRequestDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PassengerDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Passenger;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PassengerRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.AuditLogger;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.CacheManager;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
	@Autowired
	private BookingRepository bookingRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FlightRepository flightRepository;

	@Autowired
	private PassengerRepository passengerRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PromotionService promotionService;


	@Transactional
	public BookingDTO createBooking(BookingDTO bookingDTO) {
		// Validate required fields
		if (bookingDTO.getUserId() == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		if (bookingDTO.getFlightId() == null) {
			throw new IllegalArgumentException("Flight ID is required");
		}
		if (bookingDTO.getPassengers() == null || bookingDTO.getPassengers() <= 0) {
			throw new IllegalArgumentException("Invalid number of passengers (must be greater than 0)");
		}
		if (bookingDTO.getPassengers() > 10) {
			throw new IllegalArgumentException("Invalid number of passengers (maximum 10 passengers allowed)");
		}

		// Validate status if provided
		if (bookingDTO.getStatus() != null && !bookingDTO.getStatus().trim().isEmpty()) {
			try {
				Booking.Status.valueOf(bookingDTO.getStatus().toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid booking status: " + bookingDTO.getStatus());
			}
		}

		User user = userRepository.findById(bookingDTO.getUserId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Flight flight = flightRepository.findById(bookingDTO.getFlightId())
				.orElseThrow(() -> new RuntimeException("Flight not found"));

		if (flight.getSeatsAvailable() < bookingDTO.getPassengers()) {
			throw new RuntimeException("Not enough seats available");
		}

		Double totalPrice = flight.getPrice() * bookingDTO.getPassengers();
		if (bookingDTO.getBookingExtras() != null) {
			if (bookingDTO.getBookingExtras() < 0) {
				throw new IllegalArgumentException("Booking extras cannot be negative");
			}
			totalPrice += bookingDTO.getBookingExtras();
		}

		Booking booking = new Booking();
		booking.setBookingDate(new Date());
		booking.setStatus(Booking.Status.valueOf(bookingDTO.getStatus() != null ?
				bookingDTO.getStatus().toUpperCase() : "PENDING"));
		booking.setPassengers(bookingDTO.getPassengers());
		booking.setUser(user);
		booking.setFlight(flight);
		booking.setTotalPrice(totalPrice);
		booking.setBookingExtras(bookingDTO.getBookingExtras());
		booking.setPromoCode(bookingDTO.getPromoCode());

		flight.setSeatsAvailable(flight.getSeatsAvailable() - bookingDTO.getPassengers());
		flightRepository.save(flight);


		Booking savedBooking = bookingRepository.save(booking);


		AuditLogger auditLogger = AuditLogger.getInstance();
		auditLogger.logAction(
				bookingDTO.getUserId().toString(),
				"CREATE_BOOKING",
				"Booking-" + savedBooking.getId(),
				"Flight: " + bookingDTO.getFlightId() + ", Passengers: " + bookingDTO.getPassengers()
		);

		NotificationService notificationService = NotificationService.getInstance();
		notificationService.addNotification(new NotificationService.Notification(
				"EMAIL",
				bookingDTO.getUser().getEmail(),
				"Booking Created",
				"Your booking has been created successfully"
		));

		CacheManager cacheManager = CacheManager.getInstance();
		cacheManager.put("booking_" + savedBooking.getId(), savedBooking);

		return convertToDTO(savedBooking);
	}


	@Transactional
	public BookingDTO createBookingWithPassengers(BookingRequestDTO bookingRequest) {
		// Validate required fields
		if (bookingRequest.getUserId() == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		if (bookingRequest.getFlightId() == null) {
			throw new IllegalArgumentException("Flight ID is required");
		}
		if (bookingRequest.getPassengerCount() == null || bookingRequest.getPassengerCount() <= 0) {
			throw new IllegalArgumentException("Invalid number of passengers (must be greater than 0)");
		}
		if (bookingRequest.getPassengerCount() > 5) {
			throw new IllegalArgumentException("Invalid number of passengers (maximum 5 passengers allowed)");
		}
		if (bookingRequest.getPassengers() == null || bookingRequest.getPassengers().isEmpty()) {
			throw new IllegalArgumentException("Passenger details are required");
		}
		if (bookingRequest.getPassengers().size() != bookingRequest.getPassengerCount()) {
			throw new IllegalArgumentException("Number of passenger details must match passenger count");
		}

		User user = userRepository.findById(bookingRequest.getUserId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		PassengerDTO primaryPassenger = bookingRequest.getPassengers().get(0);
		if (primaryPassenger.getEmail() == null || primaryPassenger.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Primary passenger email is required");
		}
		if (!user.getEmail().equalsIgnoreCase(primaryPassenger.getEmail())) {
			throw new RuntimeException("Primary passenger email must match logged-in user email");
		}

		// Validate all passenger details
		for (int i = 0; i < bookingRequest.getPassengers().size(); i++) {
			PassengerDTO passenger = bookingRequest.getPassengers().get(i);
			if (passenger.getFirstName() == null || passenger.getFirstName().trim().isEmpty()) {
				throw new IllegalArgumentException("Passenger " + (i+1) + " first name is required");
			}
			if (passenger.getLastName() == null || passenger.getLastName().trim().isEmpty()) {
				throw new IllegalArgumentException("Passenger " + (i+1) + " last name is required");
			}
			if (passenger.getEmail() == null || passenger.getEmail().trim().isEmpty()) {
				throw new IllegalArgumentException("Passenger " + (i+1) + " email is required");
			}
			if (passenger.getDateOfBirth() == null) {
				throw new IllegalArgumentException("Passenger " + (i+1) + " date of birth is required");
			}
			if (passenger.getCountry() == null || passenger.getCountry().trim().isEmpty()) {
				throw new IllegalArgumentException("Passenger " + (i+1) + " country is required");
			}
		}

		Flight flight = flightRepository.findById(bookingRequest.getFlightId())
				.orElseThrow(() -> new RuntimeException("Flight not found"));

		if (flight.getSeatsAvailable() < bookingRequest.getPassengerCount()) {
			throw new RuntimeException("Not enough seats available");
		}

		Double baseFlightPrice = flight.getPrice() * bookingRequest.getPassengerCount();
		Double discount = 0.0;

		if (bookingRequest.getPromoCode() != null && !bookingRequest.getPromoCode().isEmpty()) {
		}

		Double flightPriceAfterDiscount = baseFlightPrice - discount;
		Double bookingExtras = bookingRequest.getBookingExtras() != null ? bookingRequest.getBookingExtras() : 0.0;
		if (bookingExtras < 0) {
			throw new IllegalArgumentException("Booking extras cannot be negative");
		}
		Double totalPrice = flightPriceAfterDiscount + bookingExtras;

		Booking booking = new Booking();
		booking.setBookingDate(new Date());
		booking.setStatus(Booking.Status.PENDING);
		booking.setPassengers(bookingRequest.getPassengerCount());
		booking.setUser(user);
		booking.setFlight(flight);
		booking.setTotalPrice(totalPrice);
		booking.setBookingExtras(bookingExtras);
		booking.setPromoCode(bookingRequest.getPromoCode());

		Booking savedBooking = bookingRepository.save(booking);

		List<Passenger> passengers = bookingRequest.getPassengers().stream().map(dto -> {
			Passenger passenger = new Passenger();
			passenger.setFirstName(dto.getFirstName().trim());
			passenger.setLastName(dto.getLastName().trim());
			passenger.setEmail(dto.getEmail().toLowerCase().trim());
			passenger.setPhone(dto.getPhone());
			passenger.setDateOfBirth(dto.getDateOfBirth());
			passenger.setCountry(dto.getCountry().trim());
			passenger.setPassportNumber(dto.getPassportNumber());
			passenger.setPassportExpiry(dto.getPassportExpiry());
			passenger.setBooking(savedBooking);
			return passenger;
		}).collect(Collectors.toList());

		passengerRepository.saveAll(passengers);
		savedBooking.setPassengerList(passengers);

		flight.setSeatsAvailable(flight.getSeatsAvailable() - bookingRequest.getPassengerCount());
		flightRepository.save(flight);

		return convertToDTO(savedBooking);
	}


	@Transactional
	public BookingDTO updateBooking(Long id, BookingDTO updatedBookingDTO) {
		// Validate ID
		if (id == null) {
			throw new IllegalArgumentException("Booking ID is required");
		}

		Booking booking = bookingRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Booking not found"));

		int oldPassengers = booking.getPassengers();
		int newPassengers = updatedBookingDTO.getPassengers() != null ?
				updatedBookingDTO.getPassengers() : oldPassengers;

		if (newPassengers <= 0) {
			throw new IllegalArgumentException("Invalid number of passengers (must be greater than 0)");
		}
		if (newPassengers > 10) {
			throw new IllegalArgumentException("Invalid number of passengers (maximum 10 passengers allowed)");
		}

		Flight flight = booking.getFlight();
		int seatDifference = newPassengers - oldPassengers;

		if (seatDifference > 0 && flight.getSeatsAvailable() < seatDifference) {
			throw new RuntimeException("Not enough seats available for update");
		}

		flight.setSeatsAvailable(flight.getSeatsAvailable() - seatDifference);
		flightRepository.save(flight);

		booking.setPassengers(newPassengers);

		if (updatedBookingDTO.getStatus() != null && !updatedBookingDTO.getStatus().trim().isEmpty()) {
			try {
				booking.setStatus(Booking.Status.valueOf(updatedBookingDTO.getStatus().toUpperCase()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid booking status: " + updatedBookingDTO.getStatus());
			}
		}

		if (updatedBookingDTO.getBookingExtras() != null) {
			if (updatedBookingDTO.getBookingExtras() < 0) {
				throw new IllegalArgumentException("Booking extras cannot be negative");
			}
			// Recalculate total price based on new booking extras
			Double newTotalPrice = flight.getPrice() * newPassengers;
			newTotalPrice += updatedBookingDTO.getBookingExtras();
			booking.setTotalPrice(newTotalPrice);
		}

		booking.setBookingExtras(updatedBookingDTO.getBookingExtras());
		booking.setPromoCode(updatedBookingDTO.getPromoCode());

		Booking updatedBooking = bookingRepository.save(booking);
		return convertToDTO(updatedBooking);
	}

	public BookingDTO getBookingById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Booking ID is required");
		}
		
		Booking booking = bookingRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Booking not found"));
		return convertToDTO(booking);
	}

	public List<BookingDTO> getAllBookings() {
		return bookingRepository.findAll().stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	public List<BookingDTO> getBookingsByStatus(String status) {
		if (status == null || status.trim().isEmpty()) {
			throw new IllegalArgumentException("Status is required");
		}
		
		try {
			Booking.Status bookingStatus = Booking.Status.valueOf(status);
			return bookingRepository.findByStatus(bookingStatus).stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid booking status: " + status);
		}
	}

	@Transactional
	public void deleteBooking(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Booking ID is required");
		}
		
		Booking booking = bookingRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Booking not found"));

		if (booking.getPayment() != null) {
			throw new RuntimeException("Cannot delete booking because it has an associated payment. Please cancel or remove the payment first.");
		}

		if (booking.getPassengerList() != null && !booking.getPassengerList().isEmpty()) {
			passengerRepository.deleteAll(booking.getPassengerList());
		}

		if (booking.getStatus() != Booking.Status.CANCELLED) {
			Flight flight = booking.getFlight();
			flight.setSeatsAvailable(flight.getSeatsAvailable() + booking.getPassengers());
			flightRepository.save(flight);
		}


		bookingRepository.delete(booking);
	}

	public List<BookingDTO> getBookingsByUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		return bookingRepository.findByUser_Id(userId).stream()
				.map(this::convertToDTO)
				.collect(Collectors.toList());
	}

	@Transactional
	public void cancelBooking(Long bookingId) {
		if (bookingId == null) {
			throw new IllegalArgumentException("Booking ID is required");
		}
		
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found"));

		if (booking.getStatus() == Booking.Status.CANCELLED) {
			throw new RuntimeException("Booking already cancelled");
		}

		Flight flight = booking.getFlight();
		flight.setSeatsAvailable(flight.getSeatsAvailable() + booking.getPassengers());
		flightRepository.save(flight);

		// If there's a payment associated with this booking, cancel it too
		if (booking.getPayment() != null) {
			Payment payment = booking.getPayment();
			payment.setStatus(Payment.Status.REFUNDED);
			paymentRepository.save(payment);
			// In a real system, you would process the actual refund here
		}

		booking.setStatus(Booking.Status.CANCELLED);
		bookingRepository.save(booking);
	}

	private BookingDTO convertToDTO(Booking booking) {
		BookingDTO dto = new BookingDTO();
		dto.setId(booking.getId());
		dto.setBookingDate(booking.getBookingDate());
		dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);
		dto.setPassengers(booking.getPassengers());
		dto.setUserId(booking.getUser() != null ? booking.getUser().getId() : null);
		dto.setFlightId(booking.getFlight() != null ? booking.getFlight().getId() : null);
		dto.setPaymentId(booking.getPayment() != null ? booking.getPayment().getId() : null);
		dto.setTotalPrice(booking.getTotalPrice());
		dto.setBookingExtras(booking.getBookingExtras());
		dto.setPromoCode(booking.getPromoCode());
		dto.setUser(booking.getUser());
		dto.setFlight(booking.getFlight());

		if (booking.getPassengerList() != null) {
			dto.setPassengerList(booking.getPassengerList().stream().map(passenger -> {
				PassengerDTO passengerDTO = new PassengerDTO();
				passengerDTO.setId(passenger.getId());
				passengerDTO.setFirstName(passenger.getFirstName());
				passengerDTO.setLastName(passenger.getLastName());
				passengerDTO.setEmail(passenger.getEmail());
				passengerDTO.setPhone(passenger.getPhone());
				passengerDTO.setDateOfBirth(passenger.getDateOfBirth());
				passengerDTO.setCountry(passenger.getCountry());
				passengerDTO.setPassportNumber(passenger.getPassportNumber());
				passengerDTO.setPassportExpiry(passenger.getPassportExpiry());
				passengerDTO.setBookingId(booking.getId());
				return passengerDTO;
			}).collect(Collectors.toList()));
		}

		return dto;
	}
}