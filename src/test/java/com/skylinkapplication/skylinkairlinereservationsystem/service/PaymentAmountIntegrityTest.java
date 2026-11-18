package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentAmountIntegrityTest {
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private PaymentService service;

    @Test
    void rejectsClientAmountBelowStoredBookingTotalAndFee() {
        assertRejectsAmount(1.0);
    }

    @Test
    void rejectsClientAmountAboveStoredBookingTotalAndFee() {
        assertRejectsAmount(500.0);
    }

    private void assertRejectsAmount(double submittedAmount) {
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setTotalPrice(300.0);
        User user = new User();
        user.setId(10L);
        booking.setUser(user);
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));
        lenient().when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentDTO request = new PaymentDTO();
        request.setBookingId(30L);
        request.setAmount(submittedAmount);
        request.setPaymentMethod("EZ_CASH");
        request.setMobile("712345678");

        assertThrows(IllegalArgumentException.class, () -> service.processPayment(request));
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }
}
