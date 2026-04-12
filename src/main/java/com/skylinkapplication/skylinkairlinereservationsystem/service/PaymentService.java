package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class PaymentService {

	@Autowired
	private PaymentRepository paymentRepository;

	public PaymentDTO processPayment(PaymentDTO paymentDTO) {
		Payment payment = new Payment();
		payment.setAmount(paymentDTO.getAmount());
		payment.setStatus(Payment.Status.COMPLETED);
		payment.setTransactionId(UUID.randomUUID().toString());
		payment.setTransactionDate(new Date());
		// Set booking (to be implemented with proper mapping)
		Payment savedPayment = paymentRepository.save(payment);
		return convertToDTO(savedPayment);
	}

	public void refundPayment(Long paymentId) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found"));
		payment.setStatus(Payment.Status.REFUNDED);
		paymentRepository.save(payment);
	}

	public Long getBookingUserId(Long paymentId) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found"));
		return payment.getBooking() != null && payment.getBooking().getUser() != null
				? payment.getBooking().getUser().getId()
				: null;
	}

	private PaymentDTO convertToDTO(Payment payment) {
		PaymentDTO dto = new PaymentDTO();
		dto.setId(payment.getId());
		dto.setAmount(payment.getAmount());
		dto.setStatus(payment.getStatus().name());
		dto.setTransactionId(payment.getTransactionId());
		dto.setTransactionDate(payment.getTransactionDate());
		return dto;
	}
}