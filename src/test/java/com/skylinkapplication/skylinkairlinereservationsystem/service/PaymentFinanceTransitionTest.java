package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentFinanceTransitionTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private PaymentService service;

    @Test
    void approvalConfirmsPendingBooking() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(40L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentDTO update = new PaymentDTO();
        update.setStatus("COMPLETED");

        PaymentDTO result = service.updatePayment(40L, update);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(Booking.Status.CONFIRMED, payment.getBooking().getStatus());
        verify(bookingRepository).save(payment.getBooking());
    }

    @Test
    void rejectionCancelsPendingBooking() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(40L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentDTO update = new PaymentDTO();
        update.setStatus("FAILED");

        PaymentDTO result = service.updatePayment(40L, update);

        assertEquals("FAILED", result.getStatus());
        assertEquals(Booking.Status.CANCELLED, payment.getBooking().getStatus());
        verify(bookingRepository).save(payment.getBooking());
    }

    private static Payment pendingPayment() {
        User user = new User();
        user.setId(10L);
        user.setUsername("traveler");
        user.setEmail("traveler@example.com");
        Flight flight = new Flight();
        flight.setFlightNumber("SK123");
        flight.setOrigin("CMB");
        flight.setDestination("LHR");
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setUser(user);
        booking.setFlight(flight);
        booking.setPassengers(1);
        booking.setStatus(Booking.Status.PENDING);
        Payment payment = new Payment();
        payment.setId(40L);
        payment.setBooking(booking);
        payment.setAmount(345.0);
        payment.setPaymentDate(new Date());
        payment.setPaymentMethod(Payment.PaymentMethod.EZ_CASH);
        payment.setTransactionId("TXN-12345678");
        payment.setStatus(Payment.Status.PENDING);
        booking.setPayment(payment);
        return payment;
    }
}
