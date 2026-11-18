package com.skylinkapplication.skylinkairlinereservationsystem.service;

import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.BookingRequestDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PassengerDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PaymentDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.dto.PromotionDTO;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Booking;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Flight;
import com.skylinkapplication.skylinkairlinereservationsystem.model.Payment;
import com.skylinkapplication.skylinkairlinereservationsystem.model.User;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.BookingRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.FlightRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PassengerRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.PaymentRepository;
import com.skylinkapplication.skylinkairlinereservationsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingPaymentBaselineTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private FlightRepository flightRepository;
    @Mock private PassengerRepository passengerRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PromotionService promotionService;

    @InjectMocks private BookingService bookingService;
    @InjectMocks private PaymentService paymentService;

    @Test
    void bookingWithPassengersSavesPendingBookingAndReservesSeats() {
        User user = user();
        Flight flight = new Flight();
        flight.setId(20L);
        flight.setPrice(150.0);
        flight.setSeatsAvailable(10);
        BookingRequestDTO request = bookingRequest(2);
        request.setPassengers(List.of(passenger("traveler@example.com"), passenger("guest@example.com")));
        request.setBookingExtras(50.0);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(flightRepository.findById(20L)).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });

        BookingDTO result = bookingService.createBookingWithPassengers(request);

        assertEquals(30L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(2, result.getPassengers());
        assertEquals(350.0, result.getTotalPrice());
        assertEquals(50.0, result.getBookingExtras());
        assertEquals(2, result.getPassengerList().size());
        assertEquals("guest@example.com", result.getPassengerList().get(1).getEmail());
        assertEquals(8, flight.getSeatsAvailable());
        verify(passengerRepository).saveAll(any());
        verify(flightRepository).save(flight);
    }

    @Test
    void bookingRejectsPassengerCountAboveTravelerLimitBeforePersistence() {
        BookingRequestDTO request = bookingRequest(6);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBookingWithPassengers(request));

        assertEquals("Invalid number of passengers (maximum 5 passengers allowed)", error.getMessage());
        verifyNoInteractions(userRepository, flightRepository, bookingRepository, passengerRepository);
    }

    @Test
    void bookingAppliesStoredPromotionToFareAndAddsExtras() {
        BookingRequestDTO request = bookingRequest(2);
        request.setPassengers(List.of(passenger("traveler@example.com"), passenger("guest@example.com")));
        request.setBookingExtras(50.0);
        request.setPromoCode("SKY20");
        Flight flight = new Flight();
        flight.setId(20L);
        flight.setPrice(150.0);
        flight.setSeatsAvailable(5);
        PromotionDTO promotion = new PromotionDTO();
        promotion.setPromoCode("SKY20");
        promotion.setDiscount(20.0);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user()));
        when(flightRepository.findById(20L)).thenReturn(Optional.of(flight));
        when(promotionService.getAllPromotions()).thenReturn(List.of(promotion));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        BookingDTO result = bookingService.createBookingWithPassengers(request);

        assertEquals("PENDING", result.getStatus());
        assertEquals("SKY20", result.getPromoCode());
        assertEquals(50.0, result.getBookingExtras());
        assertEquals(290.0, result.getTotalPrice()); // (150 × 2) less 20%, plus 50 extras
        assertEquals(3, flight.getSeatsAvailable());
    }

    @Test
    void walletSubmissionCreatesPendingPaymentWithoutConfirmingBooking() {
        Booking booking = booking();
        PaymentDTO request = walletPayment();
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(40L);
            return saved;
        });

        PaymentDTO result = paymentService.processPayment(request);

        assertEquals(40L, result.getId());
        assertEquals(30L, result.getBookingId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("EZ_CASH", result.getPaymentMethod());
        assertEquals(395.0, result.getAmount());
        assertTrue(result.getTransactionId().matches("TXN-[A-F0-9]{8}"));
        assertNotNull(result.getPaymentDate());
        assertNotNull(booking.getPayment());
        assertSame(booking, booking.getPayment().getBooking());
        assertEquals(Payment.Status.PENDING, booking.getPayment().getStatus());
        assertEquals(Booking.Status.PENDING, booking.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void duplicatePaymentIsRejectedBeforeNewPaymentIsSaved() {
        Booking booking = booking();
        booking.setPayment(new Payment());
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> paymentService.processPayment(walletPayment()));

        assertTrue(error.getMessage().contains("Payment already exists for this booking"));
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    private static BookingRequestDTO bookingRequest(int passengerCount) {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setUserId(10L);
        request.setFlightId(20L);
        request.setPassengerCount(passengerCount);
        return request;
    }

    private static PassengerDTO passenger(String email) {
        PassengerDTO passenger = new PassengerDTO();
        passenger.setFirstName("Ava");
        passenger.setLastName("Perera");
        passenger.setEmail(email);
        passenger.setPhone("0771234567");
        passenger.setDateOfBirth(new Date(946684800000L));
        passenger.setCountry("Sri Lanka");
        return passenger;
    }

    private static User user() {
        User user = new User();
        user.setId(10L);
        user.setEmail("traveler@example.com");
        return user;
    }

    private static Booking booking() {
        Booking booking = new Booking();
        booking.setId(30L);
        booking.setUser(user());
        booking.setStatus(Booking.Status.PENDING);
        booking.setTotalPrice(350.0);
        return booking;
    }

    private static PaymentDTO walletPayment() {
        PaymentDTO payment = new PaymentDTO();
        payment.setBookingId(30L);
        payment.setAmount(395.0);
        payment.setPaymentMethod("EZ_CASH");
        payment.setMobile("712345678");
        return payment;
    }
}
