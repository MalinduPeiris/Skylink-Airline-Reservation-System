package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllPayments_shouldReturnAllPayments() {
        // Arrange
        Payment payment1 = new Payment();
        payment1.setId(1L);
        payment1.setAmount(100.0);
        payment1.setStatus(Payment.Status.COMPLETED);
        payment1.setPaymentMethod(Payment.PaymentMethod.CARD);
        payment1.setTransactionId("TXN-123");
        payment1.setPaymentDate(new Date());

        Booking booking1 = new Booking();
        booking1.setId(1L);
        payment1.setBooking(booking1);

        Payment payment2 = new Payment();
        payment2.setId(2L);
        payment2.setAmount(200.0);
        payment2.setStatus(Payment.Status.FAILED);
        payment2.setPaymentMethod(Payment.PaymentMethod.EZ_CASH);
        payment2.setTransactionId("TXN-456");
        payment2.setPaymentDate(new Date());

        Booking booking2 = new Booking();
        booking2.setId(2L);
        payment2.setBooking(booking2);

        when(paymentRepository.findAll()).thenReturn(Arrays.asList(payment1, payment2));

        // Act
        List<PaymentDTO> result = paymentService.getAllPayments();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(100.0, result.get(0).getAmount());
        assertEquals("COMPLETED", result.get(0).getStatus());
        assertEquals("CARD", result.get(0).getPaymentMethod());
        assertEquals("TXN-123", result.get(0).getTransactionId());
        
        assertEquals(2L, result.get(1).getId());
        assertEquals(200.0, result.get(1).getAmount());
        assertEquals("FAILED", result.get(1).getStatus());
        assertEquals("EZ_CASH", result.get(1).getPaymentMethod());
        assertEquals("TXN-456", result.get(1).getTransactionId());

        verify(paymentRepository, times(1)).findAll();
    }
}