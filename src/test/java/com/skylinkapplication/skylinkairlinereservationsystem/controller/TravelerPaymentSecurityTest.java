package com.skylinkapplication.skylinkairlinereservationsystem.controller;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.service.BookingService;
import com.skylinkapplication.skylinkairlinereservationsystem.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelerPaymentSecurityTest {
    @Mock private PaymentService paymentService;
    @Mock private BookingService bookingService;
    @Mock private UserRepository userRepository;
    @InjectMocks private PaymentController controller;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("traveler", "unused", java.util.List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anotherTravelersBookingCannotBeViewedForPayment() {
        BookingDTO booking = otherTravelersBooking();
        when(bookingService.getBookingById(30L)).thenReturn(booking);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(currentUser()));

        assertThrows(AccessDeniedException.class,
                () -> controller.showPaymentPage(30L, new ExtendedModelMap()));
        verifyNoInteractions(paymentService);
    }

    @Test
    void anotherTravelersBookingCannotBePaid() {
        when(bookingService.getBookingById(30L)).thenReturn(otherTravelersBooking());
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(currentUser()));
        PaymentDTO request = new PaymentDTO();
        request.setBookingId(30L);
        request.setAmount(345.0);
        request.setPaymentMethod("EZ_CASH");
        request.setMobile("712345678");

        assertThrows(AccessDeniedException.class,
                () -> controller.processPayment(request, new RedirectAttributesModelMap(), new ExtendedModelMap()));
        verify(paymentService, never()).processPayment(any());
    }

    @Test
    void ownerCanViewAuthoritativePaymentTotal() {
        BookingDTO booking = ownBooking();
        when(bookingService.getBookingById(30L)).thenReturn(booking);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(currentUser()));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("traveler-payment", controller.showPaymentPage(30L, model));
        assertEquals(300.0, model.get("subtotal"));
        assertEquals(45.0, model.get("taxes"));
        assertEquals(345.0, model.get("total"));
        verifyNoInteractions(paymentService);
    }

    @Test
    void ownerSubmissionRedirectsWithPendingPaymentDetails() {
        when(bookingService.getBookingById(30L)).thenReturn(ownBooking());
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(currentUser()));
        PaymentDTO request = new PaymentDTO();
        request.setBookingId(30L);
        request.setAmount(345.0);
        request.setPaymentMethod("EZ_CASH");
        request.setMobile("712345678");
        PaymentDTO processed = new PaymentDTO();
        processed.setId(40L);
        processed.setTransactionId("TXN-12345678");
        processed.setAmount(345.0);
        processed.setStatus("PENDING");
        when(paymentService.processPayment(request)).thenReturn(processed);
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        assertEquals("redirect:/api/payment/confirmation",
                controller.processPayment(request, flash, new ExtendedModelMap()));
        assertEquals("TXN-12345678", flash.getFlashAttributes().get("transactionId"));
        assertEquals(345.0, flash.getFlashAttributes().get("amount"));
        assertNotNull(flash.getFlashAttributes().get("booking"));
        verify(paymentService).processPayment(request);
    }

    @Test
    void legacyPaymentUrlIgnoresQueryStringAmountsAndUsesCheckedPage() {
        HomeController home = new HomeController();

        assertEquals("redirect:/api/payment/page?bookingId=30",
                home.travelerPayment("30", 1.0, 0.0, 1.0, new ExtendedModelMap()));
        assertEquals("redirect:/api/user/profile",
                home.travelerPayment("invalid", 1.0, 0.0, 1.0, new ExtendedModelMap()));
    }

    private static BookingDTO ownBooking() {
        BookingDTO booking = otherTravelersBooking();
        booking.setUserId(10L);
        return booking;
    }

    private static BookingDTO otherTravelersBooking() {
        BookingDTO booking = new BookingDTO();
        booking.setId(30L);
        booking.setUserId(99L);
        booking.setTotalPrice(300.0);
        return booking;
    }

    private static User currentUser() {
        User user = new User();
        user.setId(10L);
        user.setUsername("traveler");
        return user;
    }
}
