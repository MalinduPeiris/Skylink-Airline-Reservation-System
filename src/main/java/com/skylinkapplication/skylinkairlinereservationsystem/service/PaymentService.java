package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.AuditLogger;
import com.skylinkapplication.skylinkairlinereservationsystem.singleton.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class PaymentService {
	private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private BookingRepository bookingRepository;

	private final AuditLogger auditLogger = AuditLogger.getInstance();
	private final NotificationService notificationService = NotificationService.getInstance();

	@Transactional
	public PaymentDTO processPayment(PaymentDTO paymentDTO) {
		// Validate required fields
		if (paymentDTO.getBookingId() == null) {
			throw new IllegalArgumentException("Booking ID is required");
		}
		if (paymentDTO.getAmount() == null || paymentDTO.getAmount() <= 0) {
			throw new IllegalArgumentException("Valid amount is required (must be greater than 0)");
		}
		if (paymentDTO.getPaymentMethod() == null || paymentDTO.getPaymentMethod().trim().isEmpty()) {
			throw new IllegalArgumentException("Payment method is required");
		}

		Booking booking = bookingRepository.findById(paymentDTO.getBookingId())
				.orElseThrow(() -> new RuntimeException("Booking not found with ID: " + paymentDTO.getBookingId()));

		if (booking.getPayment() != null) {
			throw new RuntimeException("Payment already exists for this booking. Booking ID: BK-" + booking.getId());
		}

		String method = paymentDTO.getPaymentMethod().toUpperCase().trim();
		if ("CARD".equals(method)) {
			validateCardPayment(paymentDTO);
		} else if ("EZ_CASH".equals(method)) {
			validateMobileWalletPayment(paymentDTO);
		} else {
			throw new IllegalArgumentException("Invalid payment method. Must be CARD or EZ_CASH");
		}

		Payment payment = new Payment();
		payment.setBooking(booking);
		payment.setAmount(paymentDTO.getAmount());
		payment.setPaymentDate(new Date());

		try {
			payment.setPaymentMethod(Payment.PaymentMethod.valueOf(method));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid payment method: " + paymentDTO.getPaymentMethod());
		}

		payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

		boolean paymentSuccess = simulatePaymentGateway(paymentDTO);

		if (paymentSuccess) {
			payment.setStatus(Payment.Status.COMPLETED);
			booking.setStatus(Booking.Status.CONFIRMED);
			logger.info("Payment successful for booking ID: {}", booking.getId());
		} else {
			payment.setStatus(Payment.Status.FAILED);
			logger.warn("Payment failed for booking ID: {}", booking.getId());
			throw new RuntimeException("Payment processing failed. Please check your payment details and try again.");
		}

		Payment savedPayment = paymentRepository.save(payment);

		booking.setPayment(savedPayment);
		bookingRepository.save(booking);

		auditLogger.logAction(
				booking.getUser().getId().toString(),
				"PAYMENT_PROCESSED",
				"Payment-" + savedPayment.getId(),
				String.format("Method: %s, Amount: Rs.%.2f, Booking: BK-%d",
						payment.getPaymentMethod(), payment.getAmount(), booking.getId())
		);

		sendPaymentConfirmationNotification(booking, savedPayment);

		logger.info("Payment processed successfully: PaymentID={}, TxnID={}, BookingID={}",
				savedPayment.getId(), savedPayment.getTransactionId(), booking.getId());

		return convertToDTO(savedPayment);
	}

	private void validateCardPayment(PaymentDTO paymentDTO) {
		if (paymentDTO.getCardNumber() == null || paymentDTO.getCardNumber().trim().isEmpty()) {
			throw new IllegalArgumentException("Card number is required");
		}
		if (paymentDTO.getCardName() == null || paymentDTO.getCardName().trim().isEmpty()) {
			throw new IllegalArgumentException("Cardholder name is required");
		}
		if (paymentDTO.getExpiry() == null || paymentDTO.getExpiry().trim().isEmpty()) {
			throw new IllegalArgumentException("Card expiry date is required");
		}
		if (paymentDTO.getCvv() == null || paymentDTO.getCvv().trim().isEmpty()) {
			throw new IllegalArgumentException("CVV is required");
		}
		if (paymentDTO.getEmail() == null || paymentDTO.getEmail().trim().isEmpty()) {
			throw new IllegalArgumentException("Email is required for card payment");
		}

		// Validate email format
		if (!isValidEmail(paymentDTO.getEmail())) {
			throw new IllegalArgumentException("Invalid email format");
		}

		// Validate expiry format (MM/YY)
		if (!paymentDTO.getExpiry().matches("^(0[1-9]|1[0-2])/\\d{2}$")) {
			throw new IllegalArgumentException("Invalid expiry format. Use MM/YY");
		}

		// Validate CVV (3 or 4 digits)
		if (!paymentDTO.getCvv().matches("^\\d{3,4}$")) {
			throw new IllegalArgumentException("CVV must be 3 or 4 digits");
		}

		// Validate card number format (13-19 digits, can include spaces)
		String cardNumber = paymentDTO.getCardNumber().replaceAll("\\s+", "");
		if (!cardNumber.matches("^\\d{13,19}$")) {
			throw new IllegalArgumentException("Invalid card number format. Must be 13-19 digits");
		}
	}

	private void validateMobileWalletPayment(PaymentDTO paymentDTO) {
		if (paymentDTO.getMobile() == null || paymentDTO.getMobile().trim().isEmpty()) {
			throw new IllegalArgumentException("Mobile number is required for eZ Cash payment");
		}

		// Validate mobile number format for Sri Lankan numbers (7XXXXXXXX)
		if (!paymentDTO.getMobile().matches("^7\\d{8}$")) {
			throw new IllegalArgumentException("Invalid mobile number format. Must be 7XXXXXXXX (9 digits starting with 7)");
		}
	}

	private boolean simulatePaymentGateway(PaymentDTO paymentDTO) {
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		String method = paymentDTO.getPaymentMethod().toUpperCase().trim();

		if ("CARD".equals(method)) {
			String cardNumber = paymentDTO.getCardNumber().replaceAll("\\s+", "");
			if (cardNumber.length() < 13 || cardNumber.length() > 19) {
				return false;
			}
			return luhnCheck(cardNumber);
		} else if ("EZ_CASH".equals(method)) {
			return paymentDTO.getMobile() != null &&
					paymentDTO.getMobile().matches("^7\\d{8}$");
		}

		return false;
	}

	private boolean luhnCheck(String cardNumber) {
		int sum = 0;
		boolean alternate = false;

		for (int i = cardNumber.length() - 1; i >= 0; i--) {
			int digit = Character.getNumericValue(cardNumber.charAt(i));

			if (alternate) {
				digit *= 2;
				if (digit > 9) {
					digit = (digit % 10) + 1;
				}
			}

			sum += digit;
			alternate = !alternate;
		}

		return (sum % 10 == 0);
	}

	private void sendPaymentConfirmationNotification(Booking booking, Payment payment) {
		String emailBody = String.format(
				"Dear %s,\n\n" +
						"Your payment has been successfully processed!\n\n" +
						"PAYMENT DETAILS:\n" +
						"Transaction ID: %s\n" +
						"Amount Paid: Rs.%.2f\n" +
						"Payment Method: %s\n" +
						"Payment Date: %s\n\n" +
						"BOOKING DETAILS:\n" +
						"Booking Reference: BK-%d\n" +
						"Flight: %s\n" +
						"Route: %s → %s\n" +
						"Passengers: %d\n" +
						"Status: CONFIRMED\n\n" +
						"Thank you for choosing Skylink Airlines!\n" +
						"We look forward to serving you.\n\n" +
						"Safe travels!",
				booking.getUser().getUsername(),
				payment.getTransactionId(),
				payment.getAmount(),
				payment.getPaymentMethod(),
				payment.getPaymentDate(),
				booking.getId(),
				booking.getFlight().getFlightNumber(),
				booking.getFlight().getOrigin(),
				booking.getFlight().getDestination(),
				booking.getPassengers()
		);

		notificationService.addNotification(
				new NotificationService.Notification(
						"EMAIL",
						booking.getUser().getEmail(),
						"Payment Confirmation - Booking BK-" + booking.getId(),
						emailBody
				)
		);
	}

	@Transactional
	public PaymentDTO createPayment(PaymentDTO paymentDTO) {
		// Validate required fields
		if (paymentDTO.getBookingId() == null) {
			throw new IllegalArgumentException("Booking ID is required");
		}
		if (paymentDTO.getAmount() == null || paymentDTO.getAmount() <= 0) {
			throw new IllegalArgumentException("Valid amount is required (must be greater than 0)");
		}
		if (paymentDTO.getStatus() == null || paymentDTO.getStatus().trim().isEmpty()) {
			throw new IllegalArgumentException("Payment status is required");
		}

		// Validate payment status
		try {
			Payment.Status.valueOf(paymentDTO.getStatus().toUpperCase().trim());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid payment status: " + paymentDTO.getStatus());
		}

		Booking booking = bookingRepository.findById(paymentDTO.getBookingId())
				.orElseThrow(() -> new RuntimeException("Booking not found with ID: " + paymentDTO.getBookingId()));

		Payment payment = new Payment();
		payment.setBooking(booking);
		payment.setAmount(paymentDTO.getAmount());
		payment.setPaymentDate(new Date());
		payment.setStatus(Payment.Status.valueOf(paymentDTO.getStatus().toUpperCase().trim()));
		payment.setPaymentMethod(Payment.PaymentMethod.CARD);
		payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

		Payment savedPayment = paymentRepository.save(payment);

		if (Payment.Status.COMPLETED.equals(savedPayment.getStatus())) {
			booking.setPayment(savedPayment);
			booking.setStatus(Booking.Status.CONFIRMED);
			bookingRepository.save(booking);
		}

		return convertToDTO(savedPayment);
	}

	public PaymentDTO getPaymentById(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Payment ID is required");
		}
		
		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));
		return convertToDTO(payment);
	}

	public List<PaymentDTO> getAllPayments() {
		try {
			List<Payment> payments = paymentRepository.findAll();
			logger.info("Retrieved {} payments from database", payments.size());
			
			// Log payment details for debugging
			for (Payment payment : payments) {
				logger.debug("Payment from DB - ID: {}, Booking ID: {}, Amount: {}, Status: {}", 
					payment.getId(), 
					payment.getBooking() != null ? payment.getBooking().getId() : null, 
					payment.getAmount(), 
					payment.getStatus());
			}
			
			List<PaymentDTO> paymentDTOs = payments.stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
					
			logger.info("Converted {} payments to DTOs", paymentDTOs.size());
			return paymentDTOs;
		} catch (Exception e) {
			logger.error("Error retrieving all payments", e);
			throw new RuntimeException("Failed to retrieve payments", e);
		}
	}

	public List<PaymentDTO> getPaymentsByStatus(String status) {
		if (status == null || status.trim().isEmpty()) {
			throw new IllegalArgumentException("Status is required");
		}
		
		try {
			Payment.Status paymentStatus = Payment.Status.valueOf(status.toUpperCase().trim());
			return paymentRepository.findByStatus(paymentStatus).stream()
					.map(this::convertToDTO)
					.collect(Collectors.toList());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid payment status: " + status);
		}
	}

	@Transactional
	public PaymentDTO updatePayment(Long id, PaymentDTO paymentDTO) {
		// Validate ID
		if (id == null) {
			throw new IllegalArgumentException("Payment ID is required");
		}

		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));

		// Validate and update fields if provided
		if (paymentDTO.getAmount() != null) {
			if (paymentDTO.getAmount() <= 0) {
				throw new IllegalArgumentException("Valid amount is required (must be greater than 0)");
			}
			payment.setAmount(paymentDTO.getAmount());
		}

		if (paymentDTO.getStatus() != null && !paymentDTO.getStatus().trim().isEmpty()) {
			try {
				payment.setStatus(Payment.Status.valueOf(paymentDTO.getStatus().toUpperCase().trim()));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid payment status: " + paymentDTO.getStatus());
			}
		}

		if (paymentDTO.getTransactionId() != null && !paymentDTO.getTransactionId().trim().isEmpty()) {
			payment.setTransactionId(paymentDTO.getTransactionId().trim().toUpperCase());
		}

		Payment updatedPayment = paymentRepository.save(payment);
		return convertToDTO(updatedPayment);
	}

	@Transactional
	public void deletePayment(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Payment ID is required");
		}

		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));

		Booking booking = payment.getBooking();
		if (booking != null) {
			booking.setPayment(null);
			booking.setStatus(Booking.Status.CANCELLED);
			bookingRepository.save(booking);
		}

		paymentRepository.delete(payment);
		logger.info("Payment deleted: ID={}", id);
	}

	@Transactional
	public void refundPayment(Long paymentId) {
		if (paymentId == null) {
			throw new IllegalArgumentException("Payment ID is required");
		}

		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		if (payment.getStatus() != Payment.Status.COMPLETED) {
			throw new RuntimeException("Only completed payments can be refunded");
		}

		payment.setStatus(Payment.Status.REFUNDED);
		paymentRepository.save(payment);

		Booking booking = payment.getBooking();
		if (booking != null) {
			booking.setStatus(Booking.Status.CANCELLED);
			bookingRepository.save(booking);
		}

		logger.info("Payment refunded: ID={}, BookingID={}", paymentId, booking != null ? booking.getId() : "N/A");
	}

	private PaymentDTO convertToDTO(Payment payment) {
		try {
			PaymentDTO dto = new PaymentDTO();
			dto.setId(payment.getId());
			dto.setBookingId(payment.getBooking() != null ? payment.getBooking().getId() : null);
			dto.setAmount(payment.getAmount());
			dto.setStatus(payment.getStatus().name());
			dto.setPaymentMethod(payment.getPaymentMethod().name());
			dto.setTransactionId(payment.getTransactionId());
			dto.setPaymentDate(payment.getPaymentDate());
			
			logger.debug("Converted payment ID {} to DTO", payment.getId());
			return dto;
		} catch (Exception e) {
			logger.error("Error converting payment to DTO for payment ID: {}", payment != null ? payment.getId() : "null", e);
			throw e;
		}
	}

	// Utility method to validate email format
	private boolean isValidEmail(String email) {
		if (email == null || email.trim().isEmpty()) {
			return false;
		}
		String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
		Pattern pattern = Pattern.compile(emailRegex);
		return pattern.matcher(email.trim()).matches();
	}
}